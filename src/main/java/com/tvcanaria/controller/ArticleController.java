package com.tvcanaria.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.tvcanaria.dto.ArticleRequest;
import com.tvcanaria.dto.ArticleResponse;

import com.tvcanaria.service.ArticleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ArticleResponse> uploadArticle(@Valid @RequestBody ArticleRequest articleRequest,
            Authentication authentication) {

        ArticleResponse article = articleService.createArticle(articleRequest, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(article);
    }

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        try {
            return ResponseEntity.ok("Endpoint is working!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error testing endpoint: " + e.getMessage());
        }
    }
}
