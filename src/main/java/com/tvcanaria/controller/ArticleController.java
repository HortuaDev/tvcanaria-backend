package com.tvcanaria.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tvcanaria.dto.article.ArticleResponse;
import com.tvcanaria.dto.article.ArticleUpdateRequest;
import com.tvcanaria.dto.article.ArticleUploadRequest;
import com.tvcanaria.service.ArticleService;

import jakarta.validation.Valid;

/**
 * Controlador REST para la gestión de artículos.
 * Base path: /api/articles
 */
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    // ------------------- POST ----------------------
    /**
     * Crea un nuevo artículo con vídeo adjunto.
     *
     * @param request        datos del artículo (multipart/form-data)
     * @param authentication usuario autenticado (ADMIN o REPORTER)
     * @return {@code 201 Created} con el artículo creado
     * @throws Exception si falla el procesamiento del vídeo
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyAuthority('ADMIN','REPORTER')")
    public ResponseEntity<ArticleResponse> uploadArticle(
            @Valid @ModelAttribute ArticleUploadRequest request,
            Authentication authentication) throws Exception {

        ArticleResponse article = articleService.createArticleWithVideo(request, authentication);

        return ResponseEntity.status(HttpStatus.CREATED).body(article);
    }

    // ------------------- GET ----------------------
    /**
     * Devuelve artículos paginados, con filtro opcional por categoría o visibilidad.
     *
     * @param categoryId  (opcional) filtra por categoría
     * @param onlyVisible (opcional) si es {@code true}, solo artículos visibles
     * @param pageable    parámetros de paginación y ordenación
     * @return {@code 200 OK} con página de artículos
     */
    @GetMapping
    public ResponseEntity<Page<ArticleResponse>> getAllArticles(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean onlyVisible,
            Pageable pageable) {

        if (categoryId != null) {
            return ResponseEntity.ok(articleService.getArticlesByCategory(categoryId, pageable));
        }

        if (Boolean.TRUE.equals(onlyVisible)) {
            return ResponseEntity.ok(articleService.getVisibleArticles(pageable));
        }

        return ResponseEntity.ok(articleService.getAllArticles(pageable));
    }

    /**
     * Obtiene un artículo por su identificador.
     *
     * @param id identificador del artículo
     * @return {@code 200 OK} con el artículo encontrado
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticleById(@PathVariable Integer id) {
        return ResponseEntity.ok(articleService.getArticleById(id));
    }

    /**
     * Devuelve artículos relacionados con el indicado (misma categoría u otros criterios).
     *
     * @param id identificador del artículo de referencia
     * @return {@code 200 OK} con lista de artículos relacionados
     */
    @GetMapping("/{id}/related")
    public ResponseEntity<List<ArticleResponse>> getRelatedArticles(@PathVariable Integer id) {
        List<ArticleResponse> relatedArticles = articleService.getRelatedArticles(id);
        return ResponseEntity.ok(relatedArticles);
    }

    /**
     * Devuelve los artículos del usuario autenticado con filtros opcionales.
     *
     * @param dateFrom       fecha de inicio (yyyy-MM-dd, opcional)
     * @param dateTo         fecha de fin (yyyy-MM-dd, opcional)
     * @param categories     lista de categorías a filtrar (opcional)
     * @param keyword        palabra clave en el título (opcional)
     * @param page           número de página (por defecto 0)
     * @param size           tamaño de página (por defecto 10)
     * @param sortBy         campo de ordenación (por defecto "date")
     * @param order          dirección de ordenación: "asc" o "desc"
     * @param authentication usuario autenticado (ADMIN o REPORTER)
     * @return {@code 200 OK} con página de artículos del usuario
     */
    @GetMapping("/my-articles")
    @PreAuthorize("hasAnyAuthority('ADMIN','REPORTER')")
    public ResponseEntity<Page<ArticleResponse>> getMyArticles(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            Authentication authentication) {

        Page<ArticleResponse> articles = articleService.getMyArticles(dateFrom, dateTo, categories, keyword, page, size,
                sortBy, order, authentication);
        return ResponseEntity.ok(articles);
    }

    /**
     * Devuelve artículos recientes de las categorías favoritas del usuario.
     *
     * @param authentication usuario autenticado
     * @return {@code 200 OK} con lista de artículos recomendados
     */
    @GetMapping("/recommended")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ArticleResponse>> getFeedFromFavoriteCategories(Authentication authentication) {
        List<ArticleResponse> articles = articleService.getRecentArticlesFromFavoriteCategories(authentication);
        return ResponseEntity.ok(articles);
    }

    /**
     * Busca artículos cuyo título contenga la palabra clave indicada.
     *
     * @param keyword término de búsqueda (parámetro de query {@code q})
     * @return {@code 200 OK} con lista de artículos coincidentes
     */
    @GetMapping("/search")
    public ResponseEntity<List<ArticleResponse>> searchArticles(@RequestParam(name = "q") String keyword) {
        List<ArticleResponse> results = articleService.searchArticlesByTitle(keyword);
        return ResponseEntity.ok(results);
    }

    /**
     * Devuelve todos los artículos publicados por un autor concreto.
     *
     * @param authorId identificador del autor
     * @return {@code 200 OK} con lista de artículos del autor
     */
    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<ArticleResponse>> getArticlesByAuthor(@PathVariable Integer authorId) {
        List<ArticleResponse> articles = articleService.getArticlesByAuthor(authorId);
        return ResponseEntity.ok(articles);
    }

    // ------------------- PUT ----------------------

    /**
     * Actualiza los datos de un artículo existente.
     *
     * @param id             identificador del artículo
     * @param request        nuevos datos del artículo (JSON)
     * @param authentication usuario autenticado; solo puede editar su propio artículo salvo ADMIN
     * @return {@code 200 OK} con el artículo actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','REPORTER')")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable Integer id,
            @Valid @RequestBody ArticleUpdateRequest request,
            Authentication authentication) {

        ArticleResponse updated = articleService.updateArticle(id, request, authentication);
        return ResponseEntity.ok(updated);
    }

    /**
     * Cambia la visibilidad (oculto/visible) de un artículo.
     *
     * @param id             identificador del artículo
     * @param hidden         {@code true} para ocultar, {@code false} para publicar
     * @param authentication usuario autenticado
     * @return {@code 200 OK} con el artículo actualizado
     */
    @PutMapping("/{id}/visibility")
    @PreAuthorize("hasAnyAuthority('ADMIN','REPORTER')")
    public ResponseEntity<ArticleResponse> changeVisibility(
            @PathVariable Integer id,
            @RequestParam Boolean hidden,
            Authentication authentication) {

        ArticleResponse updated = articleService.changeVisibility(id, hidden, authentication);
        return ResponseEntity.ok(updated);
    }

    // ------------------- DELETE ----------------------

    /**
     * Elimina un artículo por su identificador.
     *
     * @param id             identificador del artículo
     * @param authentication usuario autenticado; solo puede borrar su propio artículo salvo ADMIN
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','REPORTER')")
    public ResponseEntity<Void> deleteArticle(@PathVariable Integer id, Authentication authentication) {
        articleService.deleteArticle(id, authentication);
        return ResponseEntity.noContent().build();
    }

}
