package com.tvcanaria.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tvcanaria.dto.article.ArticleResponse;
import com.tvcanaria.dto.article.ArticleUpdateRequest;
import com.tvcanaria.dto.article.ArticleUploadRequest;
import com.tvcanaria.service.ArticleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(
            ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ArticleResponse> uploadArticle(
            @Valid @ModelAttribute ArticleUploadRequest request,
            Authentication authentication) throws Exception {

        ArticleResponse article = articleService.createArticleWithVideo(request, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(article);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable Integer id,
            @Valid @RequestBody ArticleUpdateRequest request,
            Authentication authentication) {

        ArticleResponse updated = articleService.updateArticle(id, request, authentication);
        return ResponseEntity.ok(updated);
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
            return ResponseEntity.ok(articleService.getVisibleArticleResponses());
        }

        return ResponseEntity.ok(articleService.getAllArticleResponses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticleById(@PathVariable Integer id) {
        return ResponseEntity.ok(articleService.getArticleResponseById(id));
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

    @GetMapping("/my-articles")
    @PreAuthorize("hasAnyRole('ADMIN','REPORTER')")
    public ResponseEntity<List<ArticleResponse>> getMyArticles(Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<ArticleResponse> articles;

        if (isAdmin) {
            articles = articleService.getAllArticles()
                    .stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .map(ArticleResponse::new)
                    .toList();
        } else {
            // Tomar el id del usuario autenticado directamente
            Integer userId = Integer.valueOf(authentication.getName()); // asumiendo que el token guarda userId en name
            articles = articleService.getArticlesByUserId(userId)
                    .stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .map(ArticleResponse::new)
                    .toList();
        }

        return ResponseEntity.ok(articles);
    }
}
