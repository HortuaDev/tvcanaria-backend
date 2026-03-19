package com.tvcanaria.service;

import com.tvcanaria.dto.comment.CommentRequest;
import com.tvcanaria.dto.comment.CommentResponse;
import com.tvcanaria.entity.Article;
import com.tvcanaria.entity.Comment;
import com.tvcanaria.entity.CommentReport;
import com.tvcanaria.entity.User;
import com.tvcanaria.entity.UserBlock;
import com.tvcanaria.repository.ArticleRepository;
import com.tvcanaria.repository.CommentReportRepository;
import com.tvcanaria.repository.CommentRepository;
import com.tvcanaria.repository.UserBlockRepository;
import com.tvcanaria.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentReportRepository commentReportRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

    // ---- Crear comentario
    @Transactional
    public CommentResponse createComment(CommentRequest commentRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = Integer.parseInt(authentication.getName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar si el usuario está bloqueado
        if (user.getUserBlock() != null &&
                user.getUserBlock().getBlockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Usuario bloqueado hasta "
                    + user.getUserBlock().getBlockedUntil());
        }

        Article article = articleRepository.findById(commentRequest.getArticleId())
                .orElseThrow(() -> new RuntimeException("Artículo no encontrado"));

        Comment comment = new Comment();
        comment.setComment(commentRequest.getComment());
        comment.setRating(commentRequest.getRating());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setOffenseCount(0);
        comment.setArticle(article);
        comment.setUser(user);

        Comment savedComment = commentRepository.save(comment);

        article.setRating(articleRepository.calculateAverageByArticleId(article.getArticleId()));
        articleRepository.save(article);

        return mapToCommentResponse(savedComment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByArticle(Integer articleId) {
        articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Artículo no encontrado"));

        return commentRepository.findByArticle_ArticleIdOrderByCreatedAtDesc(articleId)
                .stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments() {
        return commentRepository.findAll()
                .stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getReportedComments() {
        return commentRepository.findByOffenseCountGreaterThanEqual(5)
                .stream()
                .map(this::mapToCommentResponse)
                .toList();
    }

    @Transactional
    public void reportComment(Integer commentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = Integer.parseInt(authentication.getName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (commentReportRepository.existsByComment_CommentIdAndReporter_UserId(commentId, user.getUserId())) {
            throw new RuntimeException("Ya has reportado este comentario");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comentario no encontrado"));

        CommentReport report = new CommentReport();
        report.setComment(comment);
        report.setReporter(user);
        report.setReviewed(false);
        report.setValidReport(false);

        commentReportRepository.save(report);

        comment.setOffenseCount(comment.getOffenseCount() + 1);
        commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Integer commentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = Integer.parseInt(authentication.getName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comentario no encontrado"));

        boolean isAdmin = user.getRole() == User.Role.ADMIN;

        boolean isReporter = comment.getArticle().getAuthor().getUserId().equals(user.getUserId());

        boolean isAssignedModerator = comment.getArticle().getAuthor().getModerators().stream()
                .anyMatch(moderator -> moderator.getUserId().equals(user.getUserId()));

        boolean hasEnoughReports = comment.getOffenseCount() >= 5;

        if (isAdmin || (hasEnoughReports && (isReporter || isAssignedModerator))) {
            commentReportRepository.deleteByComment_CommentId(commentId);
            commentRepository.delete(comment);
            return;
        }

        throw new RuntimeException("No tienes permiso para eliminar este comentario");
    }

    // --- Confirmar reportes (moderador decide castigar)
    @Transactional
    public void confirmReports(Integer commentId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = Integer.parseInt(authentication.getName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comentario no encontrado"));

        boolean isAdmin = user.getRole() == User.Role.ADMIN;

        boolean isAssignedModerator = comment.getArticle().getAuthor().getModerators().stream()
                .anyMatch(moderator -> moderator.getUserId().equals(user.getUserId()));

        if (!isAdmin && !isAssignedModerator) {
            throw new RuntimeException("No tienes permisos para moderar este comentario");
        }

        if (comment.getOffenseCount() < 5) {
            throw new RuntimeException("El comentario no tiene suficientes reportes");
        }

        List<CommentReport> reports = commentReportRepository
                .findByComment_CommentId(commentId);

        for (CommentReport report : reports) {
            report.setReviewed(true);
            report.setValidReport(true);
        }

        commentReportRepository.saveAll(reports);

        User offender = comment.getUser();

        int strikes = countUserStrikes(offender);

        LocalDateTime blockUntil;

        if (strikes >= 2) {
            blockUntil = LocalDateTime.now().plusWeeks(1);
        } else {
            blockUntil = LocalDateTime.now().plusDays(1);
        }

        UserBlock block = new UserBlock();
        block.setUser(offender);
        block.setReason("Comentario inapropiado");
        block.setBlockedUntil(blockUntil);

        userBlockRepository.save(block);

        commentReportRepository.deleteByComment_CommentId(commentId);
        commentRepository.delete(comment);
    }

    // ---- Rechazar reportes a un comentario
    @Transactional
    public void rejectReports(Integer commentId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = Integer.parseInt(authentication.getName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comentario no encontrado"));

        boolean isAdmin = user.getRole() == User.Role.ADMIN;

        boolean isAssignedModerator = comment.getArticle().getAuthor().getModerators().stream()
                .anyMatch(moderator -> moderator.getUserId().equals(user.getUserId()));

        if (!isAdmin && !isAssignedModerator) {
            throw new RuntimeException("No tienes permisos para moderar este comentario");
        }

        List<CommentReport> reports = commentReportRepository
                .findByComment_CommentId(commentId);

        for (CommentReport report : reports) {
            report.setReviewed(true);
            report.setValidReport(false);
        }

        commentReportRepository.saveAll(reports);

        comment.setOffenseCount(0);
        commentRepository.save(comment);
    }

    public int countUserStrikes(User user) {
        return userBlockRepository.countByUser_UserId(user.getUserId());
    }

    private CommentResponse mapToCommentResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setCommentId(comment.getCommentId());
        response.setComment(comment.getComment());
        response.setCreatedAt(comment.getCreatedAt());
        response.setOffenseCount(comment.getOffenseCount());
        response.setRating(comment.getRating());
        response.setUsername(comment.getUser() != null ? comment.getUser().getUsername() : null);
        return response;
    }
}