package com.tvcanaria.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import com.tvcanaria.dto.article.ArticleRequest;
import com.tvcanaria.dto.article.ArticleResponse;
import com.tvcanaria.dto.article.ArticleUpdateRequest;
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

    public ArticleService(ArticleRepository articleRepository, CategoryService categoryService,
            UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.categoryService = categoryService;
        this.userRepository = userRepository;
    }

    public boolean isAuthor(Integer articleId, String username) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));
        return article.getAuthor().getUsername().equals(username);
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

    @Transactional
    public Article updateArticle(Integer id, ArticleUpdateRequest request, Authentication auth) {
        checkPermission(id, auth);

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        if (request.getTitle() != null)
            article.setTitle(request.getTitle());
        if (request.getDescription() != null)
            article.setDescription(request.getDescription());
        if (request.getLocation() != null)
            article.setLocation(request.getLocation());
        if (request.getIsHidden() != null)
            article.setIsHidden(request.getIsHidden());

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = categoryService.getCategoriesByIds(request.getCategoryIds());
            article.setCategories(categories); // JPA actualizará la tabla intermedia
        }

        return articleRepository.save(article); // esto actualiza todo
    }

    public void deleteArticle(Integer id, Authentication auth) {
        checkPermission(id, auth);

        articleRepository.deleteById(id);

    }

    public ArticleResponse createArticle(ArticleRequest request, String authentication) {

        if (authentication == null || authentication.isEmpty()) {
            throw new RuntimeException("No se ha proporcionado un ID de usuario válido");
        }

        User user = userRepository.findById(Integer.valueOf(authentication))
                .orElseThrow(() -> new RuntimeException("User not found"));

        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setDescription(request.getDescription());
        article.setVideoUrl(request.getVideo_url());
        article.setLocation(request.getLocation());
        article.setIsHidden(false);
        article.setArticleId(user.getUserId());
        article.setCreatedAt(LocalDateTime.now());
        article.setAuthor(user);
        article.setRating(null);

        if (request.getCategories() != null) {
            Set<Category> categories = categoryService.getCategoriesByIds(request.getCategories());
            article.setCategories(categories);
        } else {
            article.setCategories(new HashSet<>()); // Inicializar vacío si no hay nada
        }


        articleRepository.save(article);

        return new ArticleResponse(article.getArticleId(), article.getTitle(), article.getDescription(),
                article.getVideoUrl(), article.getLocation(), user.getUsername(), article.getRating(),
                article.getCategories());
    }

    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }

    public List<Article> getVisibleArticles() {
        return articleRepository.findByIsHiddenFalse();
    }

    public Article getArticleById(Integer id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));
    }

    public List<Article> getArticlesByCategory(Integer categoryId) {
        return articleRepository.findByCategoriesCategoryId(categoryId);
    }

    public ArticleResponse getArticleResponseById(Integer id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        
        
        return new ArticleResponse(article);
    }

    public List<ArticleResponse> getAllArticleResponses() {
        return articleRepository.findAll()
                .stream()
                .map(ArticleResponse::new)
                .collect(Collectors.toList());
    }

    public List<ArticleResponse> getVisibleArticleResponses() {
        return articleRepository.findByIsHiddenFalse()
                .stream()
                .map(ArticleResponse::new)
                .collect(Collectors.toList());
    }

}
