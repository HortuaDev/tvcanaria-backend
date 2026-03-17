package com.tvcanaria.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import com.tvcanaria.dto.article.ArticleResponse;
import com.tvcanaria.dto.article.ArticleUpdateRequest;
import com.tvcanaria.dto.article.ArticleUploadRequest;
import com.tvcanaria.entity.Article;
import com.tvcanaria.entity.Category;
import com.tvcanaria.entity.User;
import com.tvcanaria.repository.ArticleRepository;
import com.tvcanaria.repository.UserRepository;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final CloudinaryService cloudinaryService;

    public ArticleService(ArticleRepository articleRepository, CategoryService categoryService,
            UserRepository userRepository, CloudinaryService cloudinaryService) {
        this.articleRepository = articleRepository;
        this.categoryService = categoryService;
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public boolean isAuthor(Integer articleId, String username) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));
        return article.getAuthor().getUsername().equals(username);
    }

    private void checkPermission(Integer articleId, Authentication auth) {

        User user = userRepository.findById(Integer.valueOf(auth.getName()))
                .orElseThrow(() -> new RuntimeException("User not found"));

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        boolean isAuthor = article.getAuthor().getUserId().equals(user.getUserId());

        if (isAdmin)
            return;

        if (user.getRole() == User.Role.REPORTER && isAuthor)
            return;

        throw new RuntimeException("No tiene permisos para esta acción");
    }

    @Transactional
    public ArticleResponse changeVisibility(Integer id, Boolean hidden, Authentication auth) {

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        User user = userRepository.findById(Integer.valueOf(auth.getName()))
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        boolean isAuthor = article.getAuthor().getUserId().equals(user.getUserId());

        if (!isAdmin && !isAuthor) {
            throw new RuntimeException("No tiene permisos para cambiar visibilidad");
        }

        article.setIsHidden(hidden);
        articleRepository.save(article);

        return new ArticleResponse(article);
    }

    @Transactional
    public ArticleResponse updateArticle(Integer id, ArticleUpdateRequest request, Authentication auth) {

        checkPermission(id, auth);

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        if (request.getTitle() != null) {
            article.setTitle(request.getTitle());
        }

        if (request.getLocation() != null) {
            article.setLocation(request.getLocation());
        }

        if (request.getDescription() != null) {
            article.setDescription(request.getDescription());
        }

        if (request.getIsHidden() != null) {
            article.setIsHidden(request.getIsHidden());
        }

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = categoryService.getCategoriesByIds(request.getCategoryIds());
            article.setCategories(categories); // JPA actualizará la tabla intermedia
        }

        articleRepository.save(article);

        return new ArticleResponse(article);
    }

    public void deleteArticle(Integer id, Authentication auth) {
        checkPermission(id, auth);

        articleRepository.deleteById(id);

    }

    public ArticleResponse createArticleWithVideo(ArticleUploadRequest request, String authentication)
            throws Exception {

        User user = userRepository.findById(Integer.valueOf(authentication))
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != User.Role.ADMIN && user.getRole() != User.Role.REPORTER) {
            throw new RuntimeException("No permission to post new articles");
        }

        Map<String, Object> uploadResult = cloudinaryService.uploadVideo(request.getVideo());

        String videoUrl = (String) uploadResult.get("secure_url");

        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setDescription(request.getDescription());
        article.setLocation(request.getLocation());
        article.setVideoUrl(videoUrl);
        article.setIsHidden(false);
        article.setCreatedAt(LocalDateTime.now());
        article.setAuthor(user);

        if (request.getCategories() != null) {
            Set<Category> categories = categoryService.getCategoriesByIds(new HashSet<>(request.getCategories()));
            article.setCategories(categories);
        }

        articleRepository.save(article);

        return new ArticleResponse(article);
    }


    public List<Article> getArticlesByCategory(Integer categoryId) {
        return articleRepository.findByCategoriesCategoryId(categoryId);
    }

    public ArticleResponse getArticleById(Integer id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        return new ArticleResponse(article);
    }

    public List<ArticleResponse> getAllArticles() {
        return articleRepository.findAll()
                .stream()
                .map(ArticleResponse::new)
                .collect(Collectors.toList());
    }

    public List<ArticleResponse> getVisibleArticle() {
        return articleRepository.findByIsHiddenFalse()
                .stream()
                .map(ArticleResponse::new)
                .collect(Collectors.toList());
    }

}
