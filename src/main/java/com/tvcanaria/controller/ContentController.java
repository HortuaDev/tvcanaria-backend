package com.tvcanaria.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tvcanaria.entity.Article;
import com.tvcanaria.entity.User;
import com.tvcanaria.repository.ArticleRepository;
import com.tvcanaria.repository.UserRepository;
import com.tvcanaria.service.CloudinaryService;

@RestController
@RequestMapping("/content")
public class ContentController {

    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;

    public ContentController(CloudinaryService cloudinaryService, UserRepository userRepository,
            ArticleRepository articleRepository) {
        this.cloudinaryService = cloudinaryService;
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
    }

    private void checkPermission(Integer articleId, Authentication auth) {  // solo ADMIN o autor
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == User.Role.ADMIN)
            return;

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        if (article.getAuthor().getUsername().equals(username))
            return;

        throw new RuntimeException("No tiene permisos para esta acción");
    }

    @PostMapping("/{id}/upload-video")
    public ResponseEntity<String> uploadVideo(@PathVariable Integer id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        checkPermission(id, authentication);

        try {
            if (file.isEmpty())
                throw new RuntimeException("El archivo está vacío");

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

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteContent(@PathVariable Integer id, Authentication authentication) {
        checkPermission(id, authentication);

        try {
            cloudinaryService.deleteVideoFromArticle(id);
            return ResponseEntity.ok("Artículo y video eliminados correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar: " + e.getMessage());
        }
    }

}
