package com.tvcanaria.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tvcanaria.entity.Article;
import com.tvcanaria.repository.ArticleRepository;
import com.tvcanaria.service.CloudinaryService;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    private final CloudinaryService cloudinaryService;
    private final ArticleRepository articleRepository;

    public ArticleController(CloudinaryService cloudinaryService, ArticleRepository articleRepository) {
        this.cloudinaryService = cloudinaryService;
        this.articleRepository = articleRepository;
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
}
