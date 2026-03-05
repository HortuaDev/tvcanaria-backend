package com.tvcanaria.controller;

import com.tvcanaria.dto.comment.CommentRequest;
import com.tvcanaria.dto.comment.CommentResponse;
import com.tvcanaria.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    // ---- Añadir comentario
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@Valid @RequestBody CommentRequest commentRequest) {
        CommentResponse createdComment = commentService.createComment(commentRequest);
        return new ResponseEntity<>(createdComment, HttpStatus.CREATED);
    }

    // ---- Reportar comentario
    @PostMapping("/{commentId}/report")
    public ResponseEntity<?> reportComment(@PathVariable Integer commentId) {
        commentService.reportComment(commentId);
        return ResponseEntity.ok().build();
    }

    // ---- Eliminar un comentario
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Integer commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    // ---- Obtener comentarios de un vídeo
    @GetMapping("/article/{articleId}")
    public ResponseEntity<List<CommentResponse>> getCommentsByArticle(@PathVariable Integer articleId) {
        List<CommentResponse> comments = commentService.getCommentsByArticle(articleId);
        return ResponseEntity.ok(comments);
    }

    // ---- Obtener todos los comentarios
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getAllComments() {
        List<CommentResponse> comments = commentService.getComments();
        return ResponseEntity.ok(comments);
    }

}