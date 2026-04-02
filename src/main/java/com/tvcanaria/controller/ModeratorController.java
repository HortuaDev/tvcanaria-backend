package com.tvcanaria.controller;

import com.tvcanaria.dto.moderator.ModeratorRequest;
import com.tvcanaria.dto.moderator.ModeratorResponse;
import com.tvcanaria.dto.user.UserSummaryResponse;
import com.tvcanaria.service.ModeratorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de relaciones moderador-reporter.
 * Base path: /api/moderators
 */
@RestController
@RequestMapping("/api/moderators")
public class ModeratorController {

    @Autowired
    private ModeratorService moderatorService;

    // ------------------- GET ----------------------

    /**
     * Devuelve los reporters asignados a un moderador concreto.
     *
     * @param moderatorId identificador del moderador
     * @return {@code 200 OK} con la lista de reporters
     */
    @GetMapping("/{moderatorId}/reporters")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MODERATOR')")
    public ResponseEntity<List<UserSummaryResponse>> getReportersByModerator(
            @PathVariable Integer moderatorId) {
        return ResponseEntity.ok(moderatorService.getReportersByModerator(moderatorId));
    }

    /**
     * Devuelve los moderadores asignados a un reporter concreto.
     *
     * @param reporterId identificador del reporter
     * @return {@code 200 OK} con la lista de moderadores
     */
    @GetMapping("/reporters/{reporterId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'REPORTER')")
    public ResponseEntity<List<UserSummaryResponse>> getModeratorsByReporter(@PathVariable Integer reporterId) {
        return ResponseEntity.ok(moderatorService.getModeratorsByReporter(reporterId));
    }

    /**
     * Devuelve las solicitudes de moderación pendientes para el usuario autenticado.
     *
     * @param authentication usuario autenticado (MODERATOR o READER)
     * @return {@code 200 OK} con la lista de solicitudes pendientes
     */
    @GetMapping("/requests/pending")
    @PreAuthorize("hasAuthority('MODERATOR') or hasAuthority('READER')")
    public ResponseEntity<List<ModeratorResponse>> getPendingRequests(Authentication authentication) {
        return ResponseEntity.ok(moderatorService.getPendingRequests(authentication));
    }

    /**
     * Devuelve las solicitudes de moderación enviadas por el reporter autenticado.
     *
     * @param authentication usuario autenticado (REPORTER)
     * @return {@code 200 OK} con la lista de solicitudes propias
     */
    @GetMapping("/requests/my-requests")
    @PreAuthorize("hasAuthority('REPORTER')")
    public ResponseEntity<List<ModeratorResponse>> getMyRequests(Authentication authentication) {
        return ResponseEntity.ok(moderatorService.getMyRequests(authentication));
    }

    /**
     * Indica si el usuario autenticado es moderador de un reporter concreto.
     *
     * @param reporterId      identificador del reporter
     * @param authentication  usuario autenticado
     * @return {@code 200 OK} con {@code true} si es su moderador, {@code false} en caso contrario
     */
    @GetMapping("/is-moderator-of/{reporterId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> isModerator(@PathVariable Integer reporterId, Authentication authentication) {
        boolean result = moderatorService.isModeratorOf(authentication, reporterId);
        return ResponseEntity.ok(result);
    }

    /**
     * Busca un usuario por nombre o email para asignarle como reporter.
     *
     * @param query          término de búsqueda (nombre o email)
     * @param authentication usuario autenticado (REPORTER)
     * @return {@code 200 OK} con el usuario encontrado
     */
    @GetMapping("/search-user")
    @PreAuthorize("hasAuthority('REPORTER')")
    public ResponseEntity<UserSummaryResponse> searchUser(
            @RequestParam String query,
            Authentication authentication) {
        return ResponseEntity.ok(moderatorService.searchUser(query, authentication));
    }

    // ------------------- POST ----------------------

    /**
     * Envía una solicitud de moderación a otro usuario.
     *
     * @param request        datos de la solicitud (destinatario)
     * @param authentication usuario autenticado (REPORTER)
     * @return {@code 200 OK} con mensaje de confirmación
     */
    @PostMapping("/requests")
    @PreAuthorize("hasAuthority('REPORTER')")
    public ResponseEntity<?> sendRequest(@Valid @RequestBody ModeratorRequest request, Authentication authentication) {
        moderatorService.sendRequest(request, authentication);
        return ResponseEntity.ok("Solicitud enviada");
    }

    // ------------------- PUT ----------------------

    /**
     * Acepta una solicitud de moderación pendiente.
     *
     * @param id             identificador de la solicitud
     * @param authentication usuario autenticado
     * @return {@code 200 OK} con mensaje de confirmación
     */
    @PutMapping("/requests/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> acceptRequest(@PathVariable Integer id, Authentication authentication) {
        moderatorService.acceptRequest(id, authentication);
        return ResponseEntity.ok("Solicitud aceptada");
    }

    /**
     * Rechaza una solicitud de moderación pendiente.
     *
     * @param id             identificador de la solicitud
     * @param authentication usuario autenticado
     * @return {@code 200 OK} con mensaje de confirmación
     */
    @PutMapping("/requests/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> rejectRequest(@PathVariable Integer id, Authentication authentication) {
        moderatorService.rejectRequest(id, authentication);
        return ResponseEntity.ok("Solicitud rechazada");
    }

    // ------------------- DELETE ----------------------

    /**
     * Cancela una solicitud de moderación enviada por el reporter autenticado.
     *
     * @param id             identificador de la solicitud
     * @param authentication usuario autenticado (REPORTER)
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('REPORTER')")
    public ResponseEntity<?> cancelRequest(@PathVariable Integer id, Authentication authentication) {
        moderatorService.cancelRequest(id, authentication);
        return ResponseEntity.noContent().build();
    }
}