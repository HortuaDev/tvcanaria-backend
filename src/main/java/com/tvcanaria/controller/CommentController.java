package com.tvcanaria.controller;

import com.tvcanaria.dto.comment.CommentRequest;
import com.tvcanaria.dto.comment.CommentResponse;
import com.tvcanaria.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de comentarios.
 * Base path: /api/comments
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    // ------------------- POST ----------------------

    /**
     * Crea un nuevo comentario en un artículo.
     *
     * @param commentRequest datos del comentario (articleId y contenido)
     * @param authentication usuario autenticado
     * @return {@code 201 Created} con el comentario creado
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> createComment(
            @Valid @RequestBody CommentRequest commentRequest,
            Authentication authentication) {
        CommentResponse createdComment = commentService.createComment(commentRequest, authentication);
        return new ResponseEntity<>(createdComment, HttpStatus.CREATED);
    }

    /**
     * Reporta un comentario como inapropiado.
     *
     * @param commentId      identificador del comentario a reportar
     * @param authentication usuario autenticado
     * @return {@code 200 OK}
     */
    @PostMapping("/{commentId}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> reportComment(@PathVariable Integer commentId, Authentication authentication) {
        commentService.reportComment(commentId, authentication);
        return ResponseEntity.ok().build();
    }

    // ------------------- GET ----------------------

    /**
     * Devuelve los comentarios de un artículo de forma paginada.
     * Se pasa el objeto Authentication para determinar si el usuario actual puede
     * reportar.
     *
     * @param articleId      identificador del artículo
     * @param pageable       parámetros de paginación y ordenación
     * @param authentication usuario autenticado (opcional)
     * @return {@code 200 OK} con página de comentarios
     */
    @GetMapping("/article/{articleId}")
    public ResponseEntity<Page<CommentResponse>> getCommentsByArticle(
            @PathVariable Integer articleId,
            Pageable pageable,
            Authentication authentication) {
        Page<CommentResponse> comments = commentService.getCommentsByArticle(articleId, pageable, authentication);
        return ResponseEntity.ok(comments);
    }

    /**
     * Devuelve todos los comentarios de forma paginada.
     *
     * @param pageable       parámetros de paginación y ordenación
     * @param authentication usuario autenticado (opcional)
     * @return {@code 200 OK} con página de comentarios
     */
    @GetMapping
    public ResponseEntity<Page<CommentResponse>> getAllComments(Pageable pageable, Authentication authentication) {
        // Ahora pasamos authentication al service
        Page<CommentResponse> comments = commentService.getComments(pageable, authentication);
        return ResponseEntity.ok(comments);
    }

    /**
     * Devuelve los comentarios reportados con filtros opcionales.
     * Solo accesible por ADMIN o MODERATOR.
     *
     * @param dateFrom       fecha de inicio (yyyy-MM-dd, opcional)
     * @param dateTo         fecha de fin (yyyy-MM-dd, opcional)
     * @param page           número de página (por defecto 0)
     * @param size           tamaño de página (por defecto 10)
     * @param sortBy         campo de ordenación (por defecto "date")
     * @param order          dirección de ordenación: "asc" o "desc"
     * @param authentication usuario autenticado
     * @return {@code 200 OK} con página de comentarios reportados
     */
    @GetMapping("/reported")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MODERATOR') or hasAuthority('REPORTER')")
    public ResponseEntity<Page<CommentResponse>> getReportedComments(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            Authentication authentication) {
        Page<CommentResponse> comments = commentService.getReportedComments(
                dateFrom, dateTo, page, size, sortBy, order, authentication);
        return ResponseEntity.ok(comments);
    }

    // ------------------- PUT ----------------------

    /**
     * Aprueba un comentario reportado, rechazando los reportes asociados.
     *
     * @param commentId      identificador del comentario
     * @param authentication usuario autenticado (ADMIN o MODERATOR)
     * @return {@code 200 OK} con mensaje de confirmación
     */
    @PutMapping("/{commentId}/approve")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MODERATOR') or hasAuthority('REPORTER')")
    public ResponseEntity<String> approveComment(@PathVariable Integer commentId, Authentication authentication) {
        commentService.rejectReports(commentId, authentication);
        return ResponseEntity.ok("Comentario aprobado y reportes rechazados");
    }

    /**
     * Rechaza un comentario reportado, confirmando los reportes asociados.
     *
     * @param commentId      identificador del comentario
     * @param authentication usuario autenticado (ADMIN o MODERATOR)
     * @return {@code 200 OK} con mensaje de confirmación
     */
    @PutMapping("/{commentId}/reject")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MODERATOR') or hasAuthority('REPORTER')")
    public ResponseEntity<String> rejectComment(@PathVariable Integer commentId, Authentication authentication) {
        commentService.confirmReports(commentId, authentication);
        return ResponseEntity.ok("Comentario marcado como inapropiado y reportes confirmados");
    }
    // ------------------- DELETE ----------------------

    /**
     * Elimina un comentario. El usuario solo puede borrar los suyos; ADMIN puede
     * borrar cualquiera.
     *
     * @param commentId      identificador del comentario
     * @param authentication usuario autenticado
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteComment(@PathVariable Integer commentId, Authentication authentication) {
        commentService.deleteComment(commentId, authentication);
        return ResponseEntity.noContent().build();
    }
}