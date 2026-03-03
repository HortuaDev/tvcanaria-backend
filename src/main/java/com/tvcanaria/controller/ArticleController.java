package com.tvcanaria.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tvcanaria.dto.ArticleRequest;
import com.tvcanaria.dto.ArticleResponse;
import com.tvcanaria.dto.ArticleUpdateRequest;
import com.tvcanaria.entity.Article;
import com.tvcanaria.service.ArticleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(
            ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ArticleResponse> uploadArticle(@Valid @RequestBody ArticleRequest articleRequest,
            Authentication authentication) {
        ArticleResponse article = articleService.createArticle(articleRequest, authentication.getName());

        if (article != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(article);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Article> updateArticle(@PathVariable Integer id,
            @Valid @RequestBody ArticleUpdateRequest request,
            Authentication authentication) {

        Article updated = articleService.updateArticle(id, request, authentication);

        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Integer id, Authentication authentication) {
        articleService.deleteArticle(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<?> getAllArticles(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean onlyVisible) {

        if (categoryId != null) {
            return ResponseEntity.ok(articleService.getArticlesByCategory(categoryId));
        }

        if (Boolean.TRUE.equals(onlyVisible)) {
            return ResponseEntity.ok(articleService.getVisibleArticles());
        }

        return ResponseEntity.ok(articleService.getAllArticles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Article> getArticleById(@PathVariable Integer id) {
        return ResponseEntity.ok(articleService.getArticleById(id));
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
