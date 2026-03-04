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

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@Valid @RequestBody CommentRequest commentRequest) {
        CommentResponse createdComment = commentService.createComment(commentRequest);
        return new ResponseEntity<>(createdComment, HttpStatus.CREATED);
    }

    @GetMapping("/article/{articleId}")
    public ResponseEntity<List<CommentResponse>> getCommentsByArticle(@PathVariable Integer articleId) {
        List<CommentResponse> comments = commentService.getCommentsByArticle(articleId);
        return ResponseEntity.ok(comments);
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getAllComments() {
        List<CommentResponse> comments = commentService.getComments();
        return ResponseEntity.ok(comments);
    }
}