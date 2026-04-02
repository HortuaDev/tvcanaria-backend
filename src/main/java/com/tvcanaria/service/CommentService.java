package com.tvcanaria.service;

import com.tvcanaria.dto.comment.CommentRequest;
import com.tvcanaria.dto.comment.CommentResponse;
import com.tvcanaria.entity.Article;
import com.tvcanaria.entity.Comment;
import com.tvcanaria.entity.CommentReport;
import com.tvcanaria.entity.ModeratorReporter;
import com.tvcanaria.entity.User;
import com.tvcanaria.entity.UserBlock;
import com.tvcanaria.exception.AccountDisabledException;
import com.tvcanaria.exception.BadRequestException;
import com.tvcanaria.exception.DuplicateResourceException;
import com.tvcanaria.exception.ForbiddenAccessException;
import com.tvcanaria.exception.ResourceNotFoundException;
import com.tvcanaria.repository.ArticleRepository;
import com.tvcanaria.repository.CommentReportRepository;
import com.tvcanaria.repository.CommentRepository;
import com.tvcanaria.repository.UserBlockRepository;
import com.tvcanaria.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio para la gestión de comentarios: creación, consulta, moderación y eliminación.
 */
@Service
public class CommentService {

    @Autowired private CommentRepository commentRepository;
    @Autowired private CommentReportRepository commentReportRepository;
    @Autowired private ArticleRepository articleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserBlockRepository userBlockRepository;

    // ------------------- CREATE / REPORT ----------------------

    /**
     * Crea un comentario en un artículo. Verifica que el usuario no esté bloqueado
     * y actualiza la valoración media del artículo tras la inserción.
     *
     * @param commentRequest datos del comentario (articleId, texto, valoración)
     * @param auth           usuario autenticado
     * @return el comentario creado
     */
    @Transactional
    public CommentResponse createComment(CommentRequest commentRequest, Authentication auth) {
        User user = getAuthenticatedUser(auth);

        if (user.getUserBlock() != null &&
                user.getUserBlock().getBlockedUntil() != null &&
                user.getUserBlock().getBlockedUntil().isAfter(LocalDateTime.now())) {
            throw new AccountDisabledException("Tu cuenta está bloqueada temporalmente hasta: "
                    + user.getUserBlock().getBlockedUntil());
        }

        Article article = articleRepository.findById(commentRequest.getArticleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Artículo no encontrado con ID: " + commentRequest.getArticleId()));

        Comment comment = new Comment();
        comment.setComment(commentRequest.getComment());
        comment.setRating(commentRequest.getRating());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setOffenseCount(0);
        comment.setArticle(article);
        comment.setUser(user);

        Comment savedComment = commentRepository.save(comment);

        article.setRating(articleRepository.calculateAverageByArticleId(article.getArticleId()));
        articleRepository.save(article);

        return mapToCommentResponse(savedComment);
    }

    /**
     * Registra un reporte sobre un comentario. Cada usuario solo puede reportarlo una vez.
     * Incrementa el contador de ofensas del comentario.
     *
     * @param commentId identificador del comentario a reportar
     * @param auth      usuario autenticado
     */
    @Transactional
    public void reportComment(Integer commentId, Authentication auth) {
        User user = getAuthenticatedUser(auth);

        if (commentReportRepository.existsByComment_CommentIdAndReporter_UserId(commentId, user.getUserId())) {
            throw new DuplicateResourceException("Ya has reportado este comentario anteriormente");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado con ID: " + commentId));

        CommentReport report = new CommentReport();
        report.setComment(comment);
        report.setReporter(user);
        report.setReviewed(false);
        report.setValidReport(false);

        commentReportRepository.save(report);

        comment.setOffenseCount(comment.getOffenseCount() + 1);
        commentRepository.save(comment);
    }

    // ------------------- READ ----------------------

    /**
     * Devuelve los comentarios de un artículo ordenados por fecha descendente, paginados.
     *
     * @param articleId identificador del artículo
     * @param pageable  parámetros de paginación
     * @return página de comentarios del artículo
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByArticle(Integer articleId, Pageable pageable) {
        if (!articleRepository.existsById(articleId)) {
            throw new ResourceNotFoundException("Artículo no encontrado con ID: " + articleId);
        }
        return commentRepository.findByArticle_ArticleIdOrderByCreatedAtDesc(articleId, pageable)
                .map(this::mapToCommentResponse);
    }

    /**
     * Devuelve todos los comentarios paginados (sin filtros).
     *
     * @param pageable parámetros de paginación
     * @return página de comentarios
     */
    public Page<CommentResponse> getComments(Pageable pageable) {
        return commentRepository.findAll(pageable).map(this::mapToCommentResponse);
    }

    /**
     * Devuelve los comentarios reportados con filtros opcionales de fecha.
     * ADMIN ve todos; MODERATOR solo los de sus reporters asignados.
     *
     * @param dateFromStr fecha de inicio (yyyy-MM-dd, opcional)
     * @param dateToStr   fecha de fin (yyyy-MM-dd, opcional)
     * @param page        número de página
     * @param size        tamaño de página
     * @param sortBy      campo de ordenación ("date", "alphabetical" o "score")
     * @param order       dirección: "asc" o "desc"
     * @param auth        usuario autenticado (ADMIN o MODERATOR)
     * @return página de comentarios reportados
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getReportedComments(String dateFromStr, String dateToStr, int page, int size,
                                                     String sortBy, String order, Authentication auth) {

        String sortProperty = "createdAt";
        if ("alphabetical".equals(sortBy)) sortProperty = "comment";
        else if ("score".equals(sortBy)) sortProperty = "rating";

        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime dateFrom = (dateFromStr != null && !dateFromStr.trim().isEmpty())
                ? LocalDate.parse(dateFromStr, formatter).atStartOfDay() : null;
        LocalDateTime dateTo = (dateToStr != null && !dateToStr.trim().isEmpty())
                ? LocalDate.parse(dateToStr, formatter).atTime(23, 59, 59) : null;

        User user = getAuthenticatedUser(auth);

        if (user.getRole() == User.Role.ADMIN) {
            return commentRepository.findReportedCommentsWithFilters(dateFrom, dateTo, pageable)
                    .map(this::mapToCommentResponse);
        } else {
            return commentRepository.findReportedCommentsByModeratorId(
                            user.getUserId(), ModeratorReporter.Status.ACCEPTED, dateFrom, dateTo, pageable)
                    .map(this::mapToCommentResponse);
        }
    }

    // ------------------- MODERACIÓN ----------------------

    /**
     * Confirma los reportes de un comentario: marca los reportes como válidos,
     * bloquea temporalmente al autor y elimina el comentario.
     * Requiere al menos 5 reportes. Solo ADMIN o moderador asignado pueden ejecutarlo.
     *
     * @param commentId identificador del comentario
     * @param auth      usuario autenticado (ADMIN o MODERATOR)
     */
    @Transactional
    public void confirmReports(Integer commentId, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado con ID: " + commentId));

        if (!user.getRole().equals(User.Role.ADMIN) && !isAssignedModerator(comment, user))
            throw new ForbiddenAccessException("No tienes permisos para moderar los comentarios de este artículo");

        if (comment.getOffenseCount() < 5)
            throw new BadRequestException("El comentario debe tener al menos 5 reportes para poder ser sancionado");

        List<CommentReport> reports = commentReportRepository.findByComment_CommentId(commentId);
        reports.forEach(r -> { r.setReviewed(true); r.setValidReport(true); });
        commentReportRepository.saveAll(reports);

        User offender = comment.getUser();
        int strikes = countUserStrikes(offender);
        LocalDateTime blockUntil = (strikes >= 2) ? LocalDateTime.now().plusWeeks(1) : LocalDateTime.now().plusDays(1);

        UserBlock block = new UserBlock();
        block.setUser(offender);
        block.setReason("Comentario inapropiado - Violación de normas verificada");
        block.setBlockedUntil(blockUntil);
        userBlockRepository.save(block);

        commentReportRepository.deleteByComment_CommentId(commentId);
        commentRepository.delete(comment);
    }

    /**
     * Rechaza los reportes de un comentario: los marca como no válidos
     * y resetea el contador de ofensas del comentario.
     * Solo ADMIN o moderador asignado pueden ejecutarlo.
     *
     * @param commentId identificador del comentario
     * @param auth      usuario autenticado (ADMIN o MODERATOR)
     */
    @Transactional
    public void rejectReports(Integer commentId, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado con ID: " + commentId));

        if (!user.getRole().equals(User.Role.ADMIN) && !isAssignedModerator(comment, user))
            throw new ForbiddenAccessException("No tienes permisos para moderar los comentarios de este artículo");

        List<CommentReport> reports = commentReportRepository.findByComment_CommentId(commentId);
        reports.forEach(r -> { r.setReviewed(true); r.setValidReport(false); });
        commentReportRepository.saveAll(reports);

        comment.setOffenseCount(0);
        commentRepository.save(comment);
    }

    /**
     * Elimina un comentario. Puede hacerlo el propio autor, el ADMIN, el reporter
     * del artículo o el moderador asignado (estos dos últimos solo si hay ≥5 reportes).
     *
     * @param commentId identificador del comentario
     * @param auth      usuario autenticado
     */
    @Transactional
    public void deleteComment(Integer commentId, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado con ID: " + commentId));

        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        boolean isCommentAuthor = comment.getUser().getUserId().equals(user.getUserId());
        boolean isReporter = comment.getArticle().getAuthor().getUserId().equals(user.getUserId());
        boolean isAssignedModerator = comment.getArticle().getAuthor().getModeratorRelations().stream()
                .anyMatch(mr -> mr.getModerator().getUserId().equals(user.getUserId())
                        && mr.getStatus() == ModeratorReporter.Status.ACCEPTED);
        boolean hasEnoughReports = comment.getOffenseCount() >= 5;

        if (isAdmin || isCommentAuthor || (hasEnoughReports && (isReporter || isAssignedModerator))) {
            commentReportRepository.deleteByComment_CommentId(commentId);
            commentRepository.delete(comment);
            return;
        }

        throw new ForbiddenAccessException("No tienes los permisos necesarios para eliminar este comentario");
    }

    // ------------------- HELPERS ----------------------

    /**
     * Obtiene la entidad {@link User} del usuario autenticado.
     *
     * @param auth objeto de autenticación
     * @return entidad del usuario autenticado
     */
    private User getAuthenticatedUser(Authentication auth) {
        Integer userId = Integer.valueOf(auth.getName());
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    /**
     * Cuenta el número de bloqueos previos de un usuario (usado para escalar la sanción).
     *
     * @param user usuario a consultar
     * @return número de bloqueos registrados
     */
    public int countUserStrikes(User user) {
        return userBlockRepository.countByUser_UserId(user.getUserId());
    }

    /**
     * Comprueba si el usuario es el moderador asignado y aceptado del autor del artículo.
     *
     * @param comment comentario cuyo artículo se evalúa
     * @param user    usuario a verificar
     * @return {@code true} si es moderador asignado, {@code false} en caso contrario
     */
    private boolean isAssignedModerator(Comment comment, User user) {
        return comment.getArticle().getAuthor().getModeratorRelations().stream()
                .anyMatch(mr -> mr.getModerator().getUserId().equals(user.getUserId())
                        && mr.getStatus() == ModeratorReporter.Status.ACCEPTED);
    }

    /**
     * Convierte una entidad {@link Comment} en su DTO de respuesta.
     *
     * @param comment entidad a convertir
     * @return DTO {@link CommentResponse}
     */
    private CommentResponse mapToCommentResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setCommentId(comment.getCommentId());
        response.setComment(comment.getComment());
        response.setCreatedAt(comment.getCreatedAt());
        response.setOffenseCount(comment.getOffenseCount());
        response.setRating(comment.getRating());
        response.setUsername(comment.getUser() != null ? comment.getUser().getUsername() : null);
        response.setAuthorId(comment.getUser() != null ? comment.getUser().getUserId() : null);
        response.setArticleId(comment.getArticle().getArticleId());
        return response;
    }
}