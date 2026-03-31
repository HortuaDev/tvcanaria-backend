package com.tvcanaria.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @PutMapping("/{id}/visibility")
    @PreAuthorize("hasAnyAuthority('ADMIN','REPORTER')")
    public ResponseEntity<ArticleResponse> changeVisibility(
            @PathVariable Integer id,
            @RequestParam Boolean hidden,
            Authentication authentication) {

        ArticleResponse updated = articleService.changeVisibility(id, hidden, authentication);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Integer id, Authentication authentication) {
        articleService.deleteArticle(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<List<ArticleResponse>> getRelatedArticles(@PathVariable Integer id) {
        List<ArticleResponse> relatedArticles = articleService.getRelatedArticles(id);
        return ResponseEntity.ok(relatedArticles);
    }

    @GetMapping
    public ResponseEntity<Page<ArticleResponse>> getAllArticles(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean onlyVisible,
            Pageable pageable) {

        if (categoryId != null) {
            return ResponseEntity.ok(articleService.getArticlesByCategory(categoryId, pageable));
        }

        if (Boolean.TRUE.equals(onlyVisible)) {
            return ResponseEntity.ok(articleService.getVisibleArticles(pageable));
        }

        return ResponseEntity.ok(articleService.getAllArticles(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticleById(@PathVariable Integer id) {
        return ResponseEntity.ok(articleService.getArticleById(id));
    }

    @GetMapping("/my-articles")
    @PreAuthorize("hasAnyAuthority('ADMIN','REPORTER')")
    public ResponseEntity<Page<ArticleResponse>> getMyArticles(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            Authentication authentication) {

        Page<ArticleResponse> articles = articleService.getMyArticles(dateFrom, dateTo, categories, page, size, sortBy,
                order, authentication);
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/recommended")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ArticleResponse>> getFeedFromFavoriteCategories(Authentication authentication) {
        List<ArticleResponse> articles = articleService.getRecentArticlesFromFavoriteCategories(authentication);
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ArticleResponse>> searchArticles(@RequestParam(name = "q") String keyword) {
        List<ArticleResponse> results = articleService.searchArticlesByTitle(keyword);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<ArticleResponse>> getArticlesByAuthor(@PathVariable Integer authorId) {
        List<ArticleResponse> articles = articleService.getArticlesByAuthor(authorId);
        return ResponseEntity.ok(articles);
    }

}
