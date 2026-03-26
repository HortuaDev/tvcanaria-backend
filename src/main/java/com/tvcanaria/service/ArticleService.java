package com.tvcanaria.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    private void checkPermission(Article article, User user) {

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

        User user = getAuthenticatedUser(auth);

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        checkPermission(article, user);

        article.setIsHidden(hidden);

        return new ArticleResponse(articleRepository.save(article));
    }

    @Transactional
    public ArticleResponse updateArticle(Integer id, ArticleUpdateRequest request, Authentication auth) {

        User user = getAuthenticatedUser(auth);

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        checkPermission(article, user);

        if (request.getTitle() != null)
            article.setTitle(request.getTitle());
        if (request.getLocation() != null)
            article.setLocation(request.getLocation());
        if (request.getDescription() != null)
            article.setDescription(request.getDescription());
        if (request.getIsHidden() != null)
            article.setIsHidden(request.getIsHidden());

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {

            Set<Category> categories = categoryService.getCategoriesByIds(request.getCategoryIds());

            article.setCategories(categories);
        }

        return new ArticleResponse(articleRepository.save(article));
    }

    public void deleteArticle(Integer id, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        checkPermission(article, user);

        articleRepository.delete(article);
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
        // article.setArticleId(user.getUserId());
        article.setCreatedAt(LocalDateTime.now());
        article.setAuthor(user);

        if (request.getCategories() != null && !request.getCategories().isEmpty()) {

            Set<Category> categories = categoryService.getCategoriesByIds(new HashSet<>(request.getCategories()));

            article.setCategories(categories);
        }

        articleRepository.save(article);

        return new ArticleResponse(article);
    }

    public List<ArticleResponse> getMyArticles(Authentication auth) {

        User user = userRepository.findById(Integer.valueOf(auth.getName()))
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == User.Role.ADMIN) {
            return articleRepository.findAll()
                    .stream()
                    .map(ArticleResponse::new)
                    .collect(Collectors.toList());
        } else if (user.getRole() == User.Role.REPORTER) {
            return articleRepository.findByAuthorUserId(user.getUserId())
                    .stream()
                    .map(ArticleResponse::new)
                    .collect(Collectors.toList());
        } else {
            throw new RuntimeException("No permission over any articles");
        }
    }

    public List<ArticleResponse> getArticlesByCategory(Integer categoryId) {
        return articleRepository.findByCategoriesCategoryId(categoryId).stream()
                .map(ArticleResponse::new)
                .collect(Collectors.toList());
    }

    public List<ArticleResponse> getRecentArticlesFromFavoriteCategories(Authentication auth) {
        User user = getAuthenticatedUser(auth);

        Set<Category> favoriteCategories = user.getCategories();

        if (favoriteCategories == null || favoriteCategories.isEmpty()) {
            return articleRepository.findTop20ByIsHiddenFalseOrderByCreatedAtDesc()
                    .stream()
                    .map(ArticleResponse::new)
                    .collect(Collectors.toList());
        }

        return articleRepository.findTop20DistinctByCategoriesInAndIsHiddenFalseOrderByCreatedAtDesc(favoriteCategories)
                .stream()
                .map(ArticleResponse::new)
                .collect(Collectors.toList());
    }

    public List<ArticleResponse> getRelatedArticles(Integer articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        Set<Category> categories = article.getCategories();

        Pageable topTen = PageRequest.of(0, 10);

        if (categories == null || categories.isEmpty()) {
            return articleRepository.findFallbackRelatedArticles(articleId, topTen)
                    .stream()
                    .map(ArticleResponse::new)
                    .collect(Collectors.toList());
        }

        return articleRepository.findRelatedArticles(categories, articleId, topTen)
                .stream()
                .map(ArticleResponse::new)
                .collect(Collectors.toList());
    }

    public List<ArticleResponse> searchArticlesByTitle(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        return articleRepository.searchVisibleArticlesByTitle(keyword.trim())
                .stream()
                .map(ArticleResponse::new)
                .collect(Collectors.toList());
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

    public List<ArticleResponse> getVisibleArticles() {
        return articleRepository.findByIsHiddenFalse()
                .stream()
                .map(ArticleResponse::new)
                .collect(Collectors.toList());
    }

    public List<Article> getArticlesByUserId(Integer userId) {
        return articleRepository.findByAuthorUserId(userId);
    }

    private User getAuthenticatedUser(Authentication auth) {
        return userRepository.findById(Integer.valueOf(auth.getName()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
