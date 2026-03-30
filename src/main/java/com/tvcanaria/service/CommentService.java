package com.tvcanaria.service;

import com.tvcanaria.dto.comment.CommentRequest;
import com.tvcanaria.dto.comment.CommentResponse;
import com.tvcanaria.entity.Article;
import com.tvcanaria.entity.Comment;
import com.tvcanaria.entity.CommentReport;
import com.tvcanaria.entity.ModeratorReporter;
import com.tvcanaria.entity.User;
import com.tvcanaria.entity.UserBlock;
import com.tvcanaria.exception.AccountDisabledException;
import com.tvcanaria.exception.BadRequestException;
import com.tvcanaria.exception.DuplicateResourceException;
import com.tvcanaria.exception.ForbiddenAccessException;
import com.tvcanaria.exception.ResourceNotFoundException;
import com.tvcanaria.repository.ArticleRepository;
import com.tvcanaria.repository.CommentReportRepository;
import com.tvcanaria.repository.CommentRepository;
import com.tvcanaria.repository.UserBlockRepository;
import com.tvcanaria.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        // Verificar si el usuario está bloqueado temporalmente
        if (user.getUserBlock() != null &&
                user.getUserBlock().getBlockedUntil().isAfter(LocalDateTime.now())) {
            throw new AccountDisabledException("Tu cuenta está bloqueada temporalmente hasta: "
                    + user.getUserBlock().getBlockedUntil());
        }

        Article article = articleRepository.findById(commentRequest.getArticleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Artículo no encontrado con ID: " + commentRequest.getArticleId()));

        Comment comment = new Comment();
        comment.setComment(commentRequest.getComment());
        comment.setRating(commentRequest.getRating());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setOffenseCount(0);
        comment.setArticle(article);
        comment.setUser(user);

        Comment savedComment = commentRepository.save(comment);

        // Actualizar la puntuación media del artículo
        article.setRating(articleRepository.calculateAverageByArticleId(article.getArticleId()));
        articleRepository.save(article);

        return mapToCommentResponse(savedComment);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByArticle(Integer articleId, Pageable pageable) {
        if (!articleRepository.existsById(articleId)) {
            throw new ResourceNotFoundException("Artículo no encontrado con ID: " + articleId);
        }

        return commentRepository.findByArticle_ArticleIdOrderByCreatedAtDesc(articleId, pageable)
                .map(this::mapToCommentResponse);
    }

    public Page<CommentResponse> getComments(Pageable pageable) {
        return commentRepository.findAll(pageable)
                .map(this::mapToCommentResponse);
    }

    public Page<CommentResponse> getReportedComments(Pageable pageable) {
        return commentRepository.findByOffenseCountGreaterThanEqual(1, pageable)
                .map(this::mapToCommentResponse);
    }

    @Transactional
    public void reportComment(Integer commentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = Integer.parseInt(authentication.getName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        if (commentReportRepository.existsByComment_CommentIdAndReporter_UserId(commentId, user.getUserId())) {
            throw new DuplicateResourceException("Ya has reportado este comentario anteriormente");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado con ID: " + commentId));

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
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado con ID: " + commentId));

        boolean isAdmin = user.getRole() == User.Role.ADMIN;

        boolean isReporter = comment.getArticle().getAuthor().getUserId().equals(user.getUserId());

        boolean isAssignedModerator = comment.getArticle().getAuthor().getModeratorRelations().stream()
                .anyMatch(mr -> mr.getModerator().getUserId().equals(user.getUserId())
                        && mr.getStatus() == ModeratorReporter.Status.ACCEPTED);

        boolean hasEnoughReports = comment.getOffenseCount() >= 5;

        // Se puede eliminar si es Admin, o si es Reporter/Moderador pero SOLAMENTE si
        // tiene 5+ reportes
        if (isAdmin || (hasEnoughReports && (isReporter || isAssignedModerator))) {
            commentReportRepository.deleteByComment_CommentId(commentId);
            commentRepository.delete(comment);
            return;
        }

        throw new ForbiddenAccessException("No tienes los permisos necesarios para eliminar este comentario");
    }

    // --- Confirmar reportes (moderador decide castigar)
    @Transactional
    public void confirmReports(Integer commentId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = Integer.parseInt(authentication.getName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado con ID: " + commentId));

        boolean isAdmin = user.getRole() == User.Role.ADMIN;

        boolean isAssignedModerator = comment.getArticle().getAuthor().getModeratorRelations().stream()
                .anyMatch(mr -> mr.getModerator().getUserId().equals(user.getUserId())
                        && mr.getStatus() == ModeratorReporter.Status.ACCEPTED);

        if (!isAdmin && !isAssignedModerator) {
            throw new ForbiddenAccessException("No tienes permisos para moderar los comentarios de este artículo");
        }

        if (comment.getOffenseCount() < 5) {
            throw new BadRequestException("El comentario debe tener al menos 5 reportes para poder ser sancionado");
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
        block.setReason("Comentario inapropiado - Violación de normas verificada");
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
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado con ID: " + commentId));

        boolean isAdmin = user.getRole() == User.Role.ADMIN;

        boolean isAssignedModerator = comment.getArticle().getAuthor().getModeratorRelations().stream()
                .anyMatch(mr -> mr.getModerator().getUserId().equals(user.getUserId())
                        && mr.getStatus() == ModeratorReporter.Status.ACCEPTED);

        if (!isAdmin && !isAssignedModerator) {
            throw new ForbiddenAccessException("No tienes permisos para moderar los comentarios de este artículo");
        }

        List<CommentReport> reports = commentReportRepository
                .findByComment_CommentId(commentId);

        for (CommentReport report : reports) {
            report.setReviewed(true);
            report.setValidReport(false);
        }

        commentReportRepository.saveAll(reports);

        comment.setOffenseCount(0); // Limpiamos el historial de reportes
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
        response.setAuthorId(comment.getUser() != null ? comment.getUser().getUserId() : null);
        response.setArticleId(comment.getArticle().getArticleId());
        return response;
    }
}