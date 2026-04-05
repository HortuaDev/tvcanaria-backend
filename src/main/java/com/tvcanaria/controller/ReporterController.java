package com.tvcanaria.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;

import com.tvcanaria.dto.article.ArticleResponse;
import com.tvcanaria.service.ArticleService;
import com.tvcanaria.dto.profile.ReporterProfileResponse;
import com.tvcanaria.service.ReporterService;

/**
 * Controlador REST para la consulta del perfil público de un reporter.
 * Base path: /api/reporter
 */
@RestController
@RequestMapping("/api/reporter")
public class ReporterController {

    @Autowired
    private ReporterService reporterService;
    @Autowired
    private ArticleService articleService;

    /**
     * Devuelve el perfil público (canal) de un reporter.
     *
     * @param id identificador del reporter
     * @return {@code 200 OK} con el perfil del reporter,
     *         o {@code 404 Not Found} si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReporterProfileResponse> getReporterChannel(@PathVariable Integer id) {
        ReporterProfileResponse reporter = reporterService.getReporter(id);
        if (reporter == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reporter);
    }

    /**
     * Devuelve los artículos de un reporter con filtros opcionales y paginación.
     *
     * @param id         identificador del reporter
     * @param dateFrom   fecha de inicio (yyyy-MM-dd, opcional)
     * @param dateTo     fecha de fin (yyyy-MM-dd, opcional)
     * @param categories categorías separadas por coma (opcional)
     * @param page       número de página (por defecto 0)
     * @param size       tamaño de página (por defecto 12)
     * @param sortBy     campo de ordenación (por defecto "newest")
     * @param order      dirección: "asc" o "desc"
     * @return {@code 200 OK} con página de artículos
     */
    @GetMapping("/{id}/articles")
    public ResponseEntity<Page<ArticleResponse>> getReporterArticles(
            @PathVariable Integer id,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String categories,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "desc") String order) {

        Page<ArticleResponse> articles = articleService.getArticlesByAuthorWithFilters(
                id, dateFrom, dateTo, categories, page, size, sortBy, order);

        return ResponseEntity.ok(articles);
    }
}