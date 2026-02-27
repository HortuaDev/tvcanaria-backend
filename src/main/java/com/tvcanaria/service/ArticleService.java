package com.tvcanaria.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tvcanaria.dto.ArticleRequest;
import com.tvcanaria.dto.ArticleResponse;
import com.tvcanaria.dto.ArticleUpdateRequest;
import com.tvcanaria.entity.Article;
import com.tvcanaria.entity.Category;
import com.tvcanaria.entity.User;
import com.tvcanaria.repository.ArticleRepository;
import com.tvcanaria.repository.UserRepository;

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

    @Transactional
    public Article updateArticle(Integer id, ArticleUpdateRequest request) {
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

    public void deleteArticle(Integer id) {
        if (!articleRepository.existsById(id)) {
            throw new RuntimeException("Article not found");
        }
        articleRepository.deleteById(id);
    }

    public ArticleResponse createArticle(ArticleRequest request, String authentication) {

        User user = userRepository.findByUsername(authentication)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setDescription(request.getDescription());
        article.setVideoUrl(request.getVideo_url());
        article.setLocation(request.getLocation());
        article.setIsHidden(false);
        article.setCreatedAt(LocalDateTime.now());
        article.setAuthor(user);

        articleRepository.save(article);

        return new ArticleResponse(article.getArticleId(), article.getTitle(), article.getDescription(),
                article.getVideoUrl(), article.getLocation(), authentication);
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

}
