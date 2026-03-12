package com.tvcanaria.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

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

    public ArticleResponse createArticleWithVideo(
            String title,
            String description,
            String location,
            List<Integer> categoryIds,
            MultipartFile video,
            String authentication) throws Exception {

        User user = userRepository.findById(Integer.valueOf(authentication))
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> uploadResult = cloudinaryService.uploadVideo(video);

        String videoUrl = (String) uploadResult.get("secure_url");

        Article article = new Article();
        article.setTitle(title);
        article.setDescription(description);
        article.setLocation(location);
        article.setVideoUrl(videoUrl);
        article.setIsHidden(false);
        article.setCreatedAt(LocalDateTime.now());
        article.setAuthor(user);

        if (categoryIds != null) {
            Set<Category> categories = categoryService.getCategoriesByIds(new HashSet<>(categoryIds));
            article.setCategories(categories);
        }

        articleRepository.save(article);

        return new ArticleResponse(article);
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
