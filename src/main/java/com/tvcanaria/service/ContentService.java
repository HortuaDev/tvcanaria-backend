package com.tvcanaria.service;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import com.tvcanaria.entity.Article;
import com.tvcanaria.entity.User;
import com.tvcanaria.repository.ArticleRepository;
import com.tvcanaria.repository.UserRepository;

public class ContentService {

    private final CloudinaryService cloudinaryService;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public ContentService(CloudinaryService cloudinaryService, ArticleRepository articleRepository,
            UserRepository userRepository) {
        this.cloudinaryService = cloudinaryService;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    private void checkPermission(Integer articleId, Authentication auth) { // solo ADMIN o autor
        User user = userRepository.findById(Integer.valueOf(auth.getName()))
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == User.Role.ADMIN)
            return;

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        if (article.getAuthor().getUsername().equals(user.getUsername()))
            return;

        throw new RuntimeException("No tiene permisos para esta acción");
    }

    public String submitVideo(Integer articleId, MultipartFile file, Authentication username) {
        try {
            if (file.isEmpty())
                throw new RuntimeException("El archivo está vacío.");

            if (articleId == null) {
                throw new RuntimeException("El id no está incluido.");
            }

            checkPermission(articleId, username);

            Map<String, Object> uploadResult = cloudinaryService.uploadVideo(file);
            String videoUrl = uploadResult.get("url").toString();

            Article article = articleRepository.findById(articleId).orElseThrow();
            article.setVideoUrl(videoUrl);
            articleRepository.save(article);

            return videoUrl;
        } catch (Exception e) {
            System.out.println(e);
            return "";
        }

    }

    public boolean deleteVideo(Integer articleId, Authentication username) {
        checkPermission(articleId, username);

        cloudinaryService.deleteVideoFromArticle(articleId);
        articleRepository.deleteById(articleId);

        return true;
    }
}
