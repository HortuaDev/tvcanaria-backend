package com.tvcanaria.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tvcanaria.dto.ArticleRequest;
import com.tvcanaria.entity.Article;
import com.tvcanaria.repository.ArticleRepository;
import com.tvcanaria.service.CloudinaryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    private final CloudinaryService cloudinaryService;
    private final ArticleRepository articleRepository;

    public ArticleController(CloudinaryService cloudinaryService, ArticleRepository articleRepository) {
        this.cloudinaryService = cloudinaryService;
        this.articleRepository = articleRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<ArticleRequest> uploadArticle(@Valid @RequestBody ArticleRequest articleRequest) {

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
