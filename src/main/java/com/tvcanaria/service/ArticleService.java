package com.tvcanaria.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;

import com.tvcanaria.dto.article.ArticleResponse;
import com.tvcanaria.dto.article.ArticleUpdateRequest;
import com.tvcanaria.dto.article.ArticleUploadRequest;
import com.tvcanaria.entity.Article;
import com.tvcanaria.entity.Category;
import com.tvcanaria.entity.User;
import com.tvcanaria.exception.ForbiddenAccessException;
import com.tvcanaria.exception.ResourceNotFoundException;
import com.tvcanaria.repository.ArticleRepository;
import com.tvcanaria.repository.UserRepository;

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
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + articleId));
        return article.getAuthor().getUsername().equals(username);
    }

    private void checkPermission(Article article, User user) {
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        boolean isAuthor = article.getAuthor().getUserId().equals(user.getUserId());

        if (isAdmin)
            return;

        if (user.getRole() == User.Role.REPORTER && isAuthor)
            return;

        throw new ForbiddenAccessException("No tiene permisos para modificar o eliminar este artículo");
    }

    @Transactional
    public ArticleResponse changeVisibility(Integer id, Boolean hidden, Authentication auth) {
        User user = getAuthenticatedUser(auth);

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + id));

        checkPermission(article, user);

        article.setIsHidden(hidden);

        return new ArticleResponse(articleRepository.save(article));
    }

    @Transactional
    public ArticleResponse updateArticle(Integer id, ArticleUpdateRequest request, Authentication auth) {
        User user = getAuthenticatedUser(auth);

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + id));

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
            if (request.getCategoryIds().size() > 5) {
                throw new RuntimeException("Un artículo no puede tener más de 5 categorías");
            }
            Set<Category> categories = categoryService.getCategoriesByIds(request.getCategoryIds());
            article.setCategories(categories);
        }

        return new ArticleResponse(articleRepository.save(article));
    }

    @Transactional
    public void deleteArticle(Integer id, Authentication auth) {
        User user = getAuthenticatedUser(auth);

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + id));

        checkPermission(article, user);

        if (article.getVideoUrl() != null && !article.getVideoUrl().isEmpty()) {
            cloudinaryService.deleteVideoByUrl(article.getVideoUrl());
        }

        articleRepository.delete(article);
    }

    public ArticleResponse createArticleWithVideo(ArticleUploadRequest request, String authentication)
            throws Exception {

        User user = userRepository.findById(Integer.valueOf(authentication))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        if (user.getRole() != User.Role.ADMIN && user.getRole() != User.Role.REPORTER) {
            throw new ForbiddenAccessException("No tiene permisos para publicar nuevos artículos");
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

        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            // usar un set elimina duplicados si el front envia el mismo id dos veces
            Set<Integer> uniqueCategoryIds = new HashSet<>(request.getCategories());

            if (uniqueCategoryIds.size() > 5) {
                throw new IllegalArgumentException("No se pueden seleccionar más de 5 categorías.");
            }

            Set<Category> categories = categoryService.getCategoriesByIds(uniqueCategoryIds);
            article.setCategories(categories);
        }

        return new ArticleResponse(articleRepository.save(article));
    }

    public Page<ArticleResponse> getMyArticles(
            String dateFromStr,
            String dateToStr,
            List<String> categories,
            int page,
            int size,
            String sortBy,
            String order,
            Authentication auth) {

        User user = userRepository.findById(Integer.valueOf(auth.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        if (user.getRole() != User.Role.ADMIN && user.getRole() != User.Role.REPORTER) {
            throw new ForbiddenAccessException("No tienes permisos para listar artículos propios");
        }

        String sortProperty = "createdAt";
        if ("alphabetical".equals(sortBy)) {
            sortProperty = "title";
        } else if ("score".equals(sortBy)) {
            sortProperty = "rating";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        // 2. PARSEO DE FECHAS
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime dateFrom = (dateFromStr != null && !dateFromStr.trim().isEmpty())
                ? LocalDate.parse(dateFromStr, formatter).atStartOfDay()
                : null;

        LocalDateTime dateTo = (dateToStr != null && !dateToStr.trim().isEmpty())
                ? LocalDate.parse(dateToStr, formatter).atTime(23, 59, 59)
                : null;

        // 3. VALIDACIÓN DE CATEGORÍAS
        List<String> safeCategories = (categories != null && !categories.isEmpty()) ? categories : null;
        boolean hasCategories = (safeCategories != null);

        // 4. LLAMADA A BASE DE DATOS
        return articleRepository.findMyArticlesWithFilters(
                user.getUserId(),
                user.getRole().name(),
                dateFrom,
                dateTo,
                hasCategories,
                safeCategories,
                pageable).map(ArticleResponse::new);
    }

    public Page<ArticleResponse> getArticlesByCategory(Integer categoryId, Pageable pageable) {
        return articleRepository.findByCategoriesCategoryId(categoryId, pageable)
                .map(ArticleResponse::new);
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
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + articleId));

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
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + id));

        return new ArticleResponse(article);
    }

    public Page<ArticleResponse> getAllArticles(Pageable pageable) {
        return articleRepository.findAll(pageable)
                .map(ArticleResponse::new);
    }

    public Page<ArticleResponse> getVisibleArticles(Pageable pageable) {
        return articleRepository.findByIsHiddenFalse(pageable)
                .map(ArticleResponse::new);
    }

    public List<Article> getArticlesByUserId(Integer userId) {
        return articleRepository.findByAuthorUserId(userId);
    }

    private User getAuthenticatedUser(Authentication auth) {
        return userRepository.findById(Integer.valueOf(auth.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    public List<ArticleResponse> getArticlesByAuthor(Integer authorId) {
        return articleRepository.findByAuthorUserId(authorId)
                .stream()
                .map(ArticleResponse::new)
                .collect(Collectors.toList());
    }
}
