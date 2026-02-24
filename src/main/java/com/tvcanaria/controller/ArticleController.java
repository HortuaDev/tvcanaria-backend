package com.tvcanaria.controller;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tvcanaria.dto.ArticleUpdateRequest;
import com.tvcanaria.entity.Article;
import com.tvcanaria.entity.Category;
import com.tvcanaria.repository.ArticleRepository;
import com.tvcanaria.repository.CategoryRepository;
import com.tvcanaria.service.ArticleService;
import com.tvcanaria.service.CategoryService;
import com.tvcanaria.service.CloudinaryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    private final CloudinaryService cloudinaryService;
    private final ArticleRepository articleRepository;
    private final ArticleService articleService;

    public ArticleController(CloudinaryService cloudinaryService, ArticleRepository articleRepository,
            ArticleService articleService) {
        this.cloudinaryService = cloudinaryService;
        this.articleRepository = articleRepository;
        this.articleService = articleService;
    }

    @PostMapping("/{id}/upload-video")
    public ResponseEntity<String> uploadVideo(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> uploadResult = cloudinaryService.uploadVideo(file);
            String videoUrl = uploadResult.get("url").toString();

            Article article = articleRepository.findById(id).orElseThrow();
            article.setVideoUrl(videoUrl);
            articleRepository.save(article);

            return ResponseEntity.ok(videoUrl);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading video: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Article> updateArticle(
            @PathVariable Integer id,
            @Valid @RequestBody ArticleUpdateRequest request) {
        Article updated = articleService.updateArticle(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Integer id) {
        articleService.deleteArticle(id);
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
