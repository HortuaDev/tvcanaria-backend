package com.tvcanaria.controller;

import com.tvcanaria.dto.comment.CommentRequest;
import com.tvcanaria.dto.comment.CommentResponse;
import com.tvcanaria.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Page<CommentResponse>> getCommentsByArticle(
            @PathVariable Integer articleId,
            Pageable pageable) {

        Page<CommentResponse> comments = commentService.getCommentsByArticle(articleId, pageable);
        return ResponseEntity.ok(comments);
    }

    // ---- Obtener todos los comentarios
    @GetMapping
    public ResponseEntity<Page<CommentResponse>> getAllComments(Pageable pageable) {
        Page<CommentResponse> comments = commentService.getComments(pageable);
        return ResponseEntity.ok(comments);
    }

    // ---- Obtener los comentarios reportados
    @GetMapping("/reported")
    public ResponseEntity<Page<CommentResponse>> getReportedComments(Pageable pageable) {
        Page<CommentResponse> comments = commentService.getReportedComments(pageable);
        return ResponseEntity.ok(comments);
    }

    // ---- Aprobar comentario (rechazar reportes)
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @PutMapping("/{commentId}/approve")
    public ResponseEntity<String> approveComment(@PathVariable Integer commentId) {
        commentService.rejectReports(commentId);
        return ResponseEntity.ok("Comentario aprobado y reportes rechazados");
    }

    // ---- Desaprobar comentario (confirmar reportes)
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @PutMapping("/{commentId}/reject")
    public ResponseEntity<String> rejectComment(@PathVariable Integer commentId) {
        commentService.confirmReports(commentId);
        return ResponseEntity.ok("Comentario marcado como inapropiado");
    }

}