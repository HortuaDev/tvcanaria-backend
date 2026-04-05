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
import org.springframework.beans.factory.annotation.Autowired;
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
import com.tvcanaria.enums.Role;
import com.tvcanaria.exception.ForbiddenAccessException;
import com.tvcanaria.exception.ResourceNotFoundException;
import com.tvcanaria.repository.ArticleRepository;
import com.tvcanaria.repository.UserRepository;

/**
 * Servicio para la gestión de artículos: creación, consulta, edición y
 * eliminación.
 */
@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CloudinaryService cloudinaryService;

    // ------------------- CREATE ----------------------

    /**
     * Crea un nuevo artículo subiendo el vídeo a Cloudinary.
     * Admite hasta 5 categorías; lanza excepción si se superan.
     *
     * @param request datos del artículo (título, descripción, localización, vídeo,
     *                categorías)
     * @param auth    usuario autenticado (autor del artículo)
     * @return el artículo creado
     */
    public ArticleResponse createArticleWithVideo(ArticleUploadRequest request, Authentication auth)
            throws Exception {

        User user = getAuthenticatedUser(auth);

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
            Set<Integer> uniqueCategoryIds = new HashSet<>(request.getCategories());

            if (uniqueCategoryIds.size() > 5) {
                throw new IllegalArgumentException("No se pueden seleccionar más de 5 categorías.");
            }

            Set<Category> categories = categoryService.getCategoriesByIds(uniqueCategoryIds);
            article.setCategories(categories);
        }

        return new ArticleResponse(articleRepository.save(article));
    }

    // ------------------- READ ----------------------

    /**
     * Devuelve todos los artículos de forma paginada.
     *
     * @param pageable parámetros de paginación y ordenación
     * @return página de artículos
     */
    public Page<ArticleResponse> getAllArticles(Pageable pageable) {
        return articleRepository.findAll(pageable).map(ArticleResponse::new);
    }

    /**
     * Devuelve los artículos del feed público de forma paginada, con filtros
     * opcionales
     * por categoría y rango de fechas.
     *
     * @param categoryId  identificador de la categoría (opcional)
     * @param dateFromStr fecha de inicio en formato "yyyy-MM-dd" (opcional)
     * @param dateToStr   fecha de fin en formato "yyyy-MM-dd" (opcional)
     * @param pageable    parámetros de paginación y ordenación
     * @return página de artículos del feed público
     */
    public Page<ArticleResponse> getPublicFeedArticles(Integer categoryId, String dateFromStr, String dateToStr,
            Pageable pageable) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDateTime dateFrom = (dateFromStr != null && !dateFromStr.trim().isEmpty())
                ? LocalDate.parse(dateFromStr, formatter).atStartOfDay()
                : null;

        LocalDateTime dateTo = (dateToStr != null && !dateToStr.trim().isEmpty())
                ? LocalDate.parse(dateToStr, formatter).atTime(23, 59, 59)
                : null;

        return articleRepository.findFeedArticlesWithFilters(categoryId, dateFrom, dateTo, pageable)
                .map(ArticleResponse::new);
    }

    /**
     * Obtiene un artículo por su identificador.
     *
     * @param id identificador del artículo
     * @return el artículo encontrado
     */
    public ArticleResponse getArticleById(Integer id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + id));
        return new ArticleResponse(article);
    }

    /**
     * Devuelve únicamente los artículos visibles (no ocultos), paginados.
     *
     * @param pageable parámetros de paginación y ordenación
     * @return página de artículos visibles
     */
    public Page<ArticleResponse> getVisibleArticles(Pageable pageable) {
        return articleRepository.findByIsHiddenFalse(pageable).map(ArticleResponse::new);
    }

    /**
     * Devuelve los artículos pertenecientes a una categoría, paginados.
     *
     * @param categoryId identificador de la categoría
     * @param pageable   parámetros de paginación y ordenación
     * @return página de artículos de la categoría
     */
    public Page<ArticleResponse> getArticlesByCategory(Integer categoryId, Pageable pageable) {
        return articleRepository.findByCategoriesCategoryId(categoryId, pageable).map(ArticleResponse::new);
    }

    /**
     * Devuelve todos los artículos publicados por un autor concreto.
     *
     * @param authorId identificador del autor
     * @return lista de artículos del autor
     */
    public List<ArticleResponse> getArticlesByAuthor(Integer authorId) {
        return articleRepository.findByAuthorUserId(authorId)
                .stream().map(ArticleResponse::new).collect(Collectors.toList());
    }

    /**
     * Devuelve los artículos del usuario autenticado con filtros opcionales de
     * fecha,
     * categorías y palabra clave. ADMIN ve todos; REPORTER solo los suyos.
     *
     * @param dateFromStr fecha de inicio (yyyy-MM-dd, opcional)
     * @param dateToStr   fecha de fin (yyyy-MM-dd, opcional)
     * @param categories  lista de nombres de categorías a filtrar (opcional)
     * @param keyword     palabra clave en el título (opcional)
     * @param page        número de página
     * @param size        tamaño de página
     * @param sortBy      campo de ordenación ("date", "alphabetical" o "score")
     * @param order       dirección: "asc" o "desc"
     * @param auth        usuario autenticado
     * @return página de artículos filtrados
     */
    public Page<ArticleResponse> getMyArticles(
            String dateFromStr, String dateToStr, List<String> categories,
            String keyword, int page, int size, String sortBy, String order, Authentication auth) {

        User user = getAuthenticatedUser(auth);

        String sortProperty = "createdAt";
        if ("alphabetical".equals(sortBy))
            sortProperty = "title";
        else if ("score".equals(sortBy))
            sortProperty = "rating";

        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime dateFrom = (dateFromStr != null && !dateFromStr.trim().isEmpty())
                ? LocalDate.parse(dateFromStr, formatter).atStartOfDay()
                : null;
        LocalDateTime dateTo = (dateToStr != null && !dateToStr.trim().isEmpty())
                ? LocalDate.parse(dateToStr, formatter).atTime(23, 59, 59)
                : null;

        List<String> safeCategories = (categories != null && !categories.isEmpty()) ? categories : null;
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        return articleRepository.findMyArticlesWithFilters(
                user.getUserId(), user.getRole().name(),
                dateFrom, dateTo, safeCategories != null, safeCategories, searchKeyword,
                pageable).map(ArticleResponse::new);
    }

    /**
     * Devuelve hasta 20 artículos recientes de las categorías favoritas del
     * usuario.
     * Si no tiene favoritas, devuelve los 20 artículos más recientes visibles.
     *
     * @param auth usuario autenticado
     * @return lista de artículos recomendados
     */
    public List<ArticleResponse> getRecentArticlesFromFavoriteCategories(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        Set<Category> favoriteCategories = user.getCategories();

        if (favoriteCategories == null || favoriteCategories.isEmpty()) {
            return articleRepository.findTop20ByIsHiddenFalseOrderByCreatedAtDesc()
                    .stream().map(ArticleResponse::new).collect(Collectors.toList());
        }

        return articleRepository.findTop20DistinctByCategoriesInAndIsHiddenFalseOrderByCreatedAtDesc(favoriteCategories)
                .stream().map(ArticleResponse::new).collect(Collectors.toList());
    }

    /**
     * Devuelve hasta 10 artículos relacionados con el indicado (misma categoría).
     * Si el artículo no tiene categorías, usa un criterio de fallback.
     *
     * @param articleId identificador del artículo de referencia
     * @return lista de artículos relacionados
     */
    public List<ArticleResponse> getRelatedArticles(Integer articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + articleId));

        Set<Category> categories = article.getCategories();
        Pageable topTen = PageRequest.of(0, 10);

        if (categories == null || categories.isEmpty()) {
            return articleRepository.findFallbackRelatedArticles(articleId, topTen)
                    .stream().map(ArticleResponse::new).collect(Collectors.toList());
        }

        return articleRepository.findRelatedArticles(categories, articleId, topTen)
                .stream().map(ArticleResponse::new).collect(Collectors.toList());
    }

    /**
     * Busca artículos visibles cuyo título contenga la palabra clave.
     * Devuelve lista vacía si el término es nulo o en blanco.
     *
     * @param keyword término de búsqueda
     * @return lista de artículos coincidentes
     */
    public List<ArticleResponse> searchArticlesByTitle(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return articleRepository.searchVisibleArticlesByTitle(keyword.trim())
                .stream().map(ArticleResponse::new).collect(Collectors.toList());
    }

    // ------------------- EDIT ----------------------

    /**
     * Cambia la visibilidad (oculto/visible) de un artículo.
     * Solo el autor o un ADMIN pueden realizarlo.
     *
     * @param id     identificador del artículo
     * @param hidden {@code true} para ocultar, {@code false} para publicar
     * @param auth   usuario autenticado
     * @return el artículo actualizado
     */
    @Transactional
    public ArticleResponse changeVisibility(Integer id, Boolean hidden, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + id));
        checkPermission(article, user);
        article.setIsHidden(hidden);
        return new ArticleResponse(articleRepository.save(article));
    }

    /**
     * Actualiza los campos editables de un artículo (título, descripción,
     * localización,
     * visibilidad y categorías). Solo el autor o un ADMIN pueden realizarlo.
     * Las categorías no pueden superar 5.
     *
     * @param id      identificador del artículo
     * @param request nuevos datos del artículo
     * @param auth    usuario autenticado
     * @return el artículo actualizado
     */
    @Transactional
    public ArticleResponse updateArticle(Integer id, ArticleUpdateRequest request, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + id));
        checkPermission(article, user);

        if (request.getTitle() != null && !request.getTitle().isBlank())
            article.setTitle(request.getTitle());
        if (request.getLocation() != null && !request.getLocation().isBlank())
            article.setLocation(request.getLocation());
        if (request.getDescription() != null)
            article.setDescription(request.getDescription());
        if (request.getIsHidden() != null)
            article.setIsHidden(request.getIsHidden());

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            if (request.getCategoryIds().size() > 5)
                throw new IllegalArgumentException("Un artículo no puede tener más de 5 categorías");
            article.setCategories(categoryService.getCategoriesByIds(request.getCategoryIds()));
        }

        return new ArticleResponse(articleRepository.save(article));
    }

    // ------------------- DELETE ----------------------

    /**
     * Elimina un artículo y su vídeo asociado en Cloudinary (borrado asíncrono).
     * Solo el autor o un ADMIN pueden realizarlo.
     *
     * @param id   identificador del artículo
     * @param auth usuario autenticado
     */
    @Transactional
    public void deleteArticle(Integer id, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado con ID: " + id));
        checkPermission(article, user);

        String videoUrl = article.getVideoUrl();
        articleRepository.delete(article);

        if (videoUrl != null && !videoUrl.isEmpty()) {
            cloudinaryService.deleteVideoByUrl(videoUrl);
        }
    }

    // ------------------- HELPERS ----------------------

    /**
     * Extrae el ID del usuario autenticado desde el objeto {@link Authentication}.
     *
     * @param auth objeto de autenticación
     * @return ID del usuario autenticado
     */
    private Integer getAuthenticatedUserId(Authentication auth) {
        return Integer.valueOf(auth.getName());
    }

    /**
     * Obtiene la entidad {@link User} del usuario autenticado.
     *
     * @param auth objeto de autenticación
     * @return entidad del usuario autenticado
     */
    private User getAuthenticatedUser(Authentication auth) {
        return userRepository.findById(getAuthenticatedUserId(auth))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    /**
     * Verifica que el usuario tenga permisos sobre el artículo.
     * ADMIN puede operar sobre cualquier artículo; REPORTER solo sobre los suyos.
     *
     * @param article artículo sobre el que se opera
     * @param user    usuario que realiza la acción
     */
    private void checkPermission(Article article, User user) {
        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isAuthor = article.getAuthor().getUserId().equals(user.getUserId());

        if (isAdmin)
            return;

        if (user.getRole() == Role.REPORTER && isAuthor)
            return;

        throw new ForbiddenAccessException("No tiene permisos para modificar o eliminar este artículo");
    }
}