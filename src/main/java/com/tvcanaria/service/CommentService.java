package com.tvcanaria.service;

import com.tvcanaria.dto.comment.CommentRequest;
import com.tvcanaria.dto.comment.CommentResponse;
import com.tvcanaria.entity.Article;
import com.tvcanaria.entity.Comment;
import com.tvcanaria.entity.User;
import com.tvcanaria.repository.ArticleRepository;
import com.tvcanaria.repository.CommentRepository;
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
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;


    @Transactional
    public CommentResponse createComment(CommentRequest commentRequest) {
        // Obtener el usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        User user = userRepository.findById(Integer.parseInt(userId))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Buscar el artículo
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
        // Verificar que el artículo existe
        articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Artículo no encontrado"));

        return commentRepository.findByArticle_ArticleIdOrderByCreatedAtDesc(articleId).stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments() {
        return commentRepository.findAll().stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());
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