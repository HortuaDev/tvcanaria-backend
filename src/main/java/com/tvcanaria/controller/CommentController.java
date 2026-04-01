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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    // ------------------- POST ----------------------

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> createComment(
            @Valid @RequestBody CommentRequest commentRequest,
            Authentication authentication) {
        CommentResponse createdComment = commentService.createComment(commentRequest, authentication);
        return new ResponseEntity<>(createdComment, HttpStatus.CREATED);
    }

    @PostMapping("/{commentId}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> reportComment(@PathVariable Integer commentId, Authentication authentication) {
        commentService.reportComment(commentId, authentication);
        return ResponseEntity.ok().build();
    }

    // ------------------- GET ----------------------

    @GetMapping("/article/{articleId}")
    public ResponseEntity<Page<CommentResponse>> getCommentsByArticle(
            @PathVariable Integer articleId,
            Pageable pageable) {
        Page<CommentResponse> comments = commentService.getCommentsByArticle(articleId, pageable);
        return ResponseEntity.ok(comments);
    }

    @GetMapping
    public ResponseEntity<Page<CommentResponse>> getAllComments(Pageable pageable) {
        Page<CommentResponse> comments = commentService.getComments(pageable);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/reported")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MODERATOR')")
    public ResponseEntity<Page<CommentResponse>> getReportedComments(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            Authentication authentication) {

        Page<CommentResponse> comments = commentService.getReportedComments(
                dateFrom, dateTo, page, size, sortBy, order, authentication);
        return ResponseEntity.ok(comments);
    }

    // ------------------- PUT ----------------------

    @PutMapping("/{commentId}/approve")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MODERATOR')")
    public ResponseEntity<String> approveComment(@PathVariable Integer commentId, Authentication authentication) {
        commentService.rejectReports(commentId, authentication);
        return ResponseEntity.ok("Comentario aprobado y reportes rechazados");
    }

    @PutMapping("/{commentId}/reject")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MODERATOR')")
    public ResponseEntity<String> rejectComment(@PathVariable Integer commentId, Authentication authentication) {
        commentService.confirmReports(commentId, authentication);
        return ResponseEntity.ok("Comentario marcado como inapropiado y reportes confirmados");
    }

    // ------------------- DELETE ----------------------

    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteComment(@PathVariable Integer commentId, Authentication authentication) {
        commentService.deleteComment(commentId, authentication);
        return ResponseEntity.noContent().build();
    }
}