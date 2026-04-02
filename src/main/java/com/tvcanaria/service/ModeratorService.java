package com.tvcanaria.service;

import com.tvcanaria.dto.moderator.ModeratorRequest;
import com.tvcanaria.dto.moderator.ModeratorResponse;
import com.tvcanaria.dto.user.UserSummaryResponse;
import com.tvcanaria.entity.ModeratorReporter;
import com.tvcanaria.entity.User;
import com.tvcanaria.enums.Role;
import com.tvcanaria.enums.Status;
import com.tvcanaria.exception.BadRequestException;
import com.tvcanaria.exception.DuplicateResourceException;
import com.tvcanaria.exception.ForbiddenAccessException;
import com.tvcanaria.exception.ResourceNotFoundException;
import com.tvcanaria.repository.ModeratorReporterRepository;
import com.tvcanaria.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de relaciones moderador-reporter:
 * asignaciones, solicitudes y consultas.
 */
@Service
public class ModeratorService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModeratorReporterRepository moderatorReporterRepository;

    // ------------------- ASSIGNMENTS (Admin actions) ----------------------

    /**
     * Asigna un reporter a un moderador creando la relación con estado ACCEPTED.
     * Valida los roles de ambos usuarios y que la relación no exista previamente.
     *
     * @param moderatorId identificador del moderador
     * @param reporterId  identificador del reporter
     */
    @Transactional
    public void assignReporterToModerator(Integer moderatorId, Integer reporterId) {
        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Moderador no encontrado con ID: " + moderatorId));

        if (moderator.getRole() != Role.MODERATOR && moderator.getRole() != Role.ADMIN) {
            throw new BadRequestException("El usuario seleccionado no tiene el rol de moderador");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Reportero no encontrado con ID: " + reporterId));

        if (reporter.getRole() != Role.REPORTER) {
            throw new BadRequestException("El usuario seleccionado no tiene el rol de reportero");
        }

        moderatorReporterRepository
                .findByModerator_UserIdAndReporter_UserId(moderatorId, reporterId)
                .ifPresent(r -> {
                    throw new DuplicateResourceException("Ya existe una relación entre estos usuarios");
                });

        ModeratorReporter relation = new ModeratorReporter();
        relation.setModerator(moderator);
        relation.setReporter(reporter);
        relation.setStatus(Status.ACCEPTED);
        moderatorReporterRepository.save(relation);
    }

    /**
     * Elimina la relación de moderación entre un moderador y un reporter.
     *
     * @param moderatorId identificador del moderador
     * @param reporterId  identificador del reporter
     */
    @Transactional
    public void removeReporterFromModerator(Integer moderatorId, Integer reporterId) {
        ModeratorReporter relation = moderatorReporterRepository
                .findByModerator_UserIdAndReporter_UserId(moderatorId, reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Relación no encontrada entre estos usuarios"));

        moderatorReporterRepository.delete(relation);
    }

    // ------------------- READ ----------------------

    /**
     * Devuelve los reporters aceptados asignados a un moderador.
     *
     * @param moderatorId identificador del moderador
     * @return lista de reporters asignados
     */
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getReportersByModerator(Integer moderatorId) {
        if (!userRepository.existsById(moderatorId))
            throw new ResourceNotFoundException("Moderador no encontrado con ID: " + moderatorId);

        return moderatorReporterRepository
                .findAcceptedReportersByModerator(moderatorId, Status.ACCEPTED)
                .stream().map(this::mapToUserSummary).collect(Collectors.toList());
    }

    /**
     * Devuelve los moderadores aceptados de un reporter.
     *
     * @param reporterId identificador del reporter
     * @return lista de moderadores asignados
     */
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getModeratorsByReporter(Integer reporterId) {
        if (!userRepository.existsById(reporterId))
            throw new ResourceNotFoundException("Reportero no encontrado con ID: " + reporterId);

        return moderatorReporterRepository
                .findAcceptedModeratorsByReporter(reporterId, Status.ACCEPTED)
                .stream().map(this::mapToUserSummary).collect(Collectors.toList());
    }

    /**
     * Devuelve las solicitudes de moderación pendientes dirigidas al usuario
     * autenticado.
     *
     * @param auth usuario autenticado (destinatario de las solicitudes)
     * @return lista de solicitudes pendientes
     */
    @Transactional(readOnly = true)
    public List<ModeratorResponse> getPendingRequests(Authentication auth) {
        Integer moderatorId = getAuthenticatedUserId(auth);
        return moderatorReporterRepository
                .findByModerator_UserIdAndStatus(moderatorId, Status.PENDING)
                .stream().map(ModeratorResponse::new).collect(Collectors.toList());
    }

    /**
     * Indica si el usuario autenticado es el moderador aceptado de un reporter
     * concreto.
     *
     * @param auth       usuario autenticado
     * @param reporterId identificador del reporter
     * @return {@code true} si la relación existe y está aceptada
     */
    @Transactional(readOnly = true)
    public boolean isModeratorOf(Authentication auth, Integer reporterId) {
        Integer moderatorId = getAuthenticatedUserId(auth);
        return moderatorReporterRepository
                .findByModerator_UserIdAndReporter_UserId(moderatorId, reporterId)
                .map(r -> r.getStatus() == Status.ACCEPTED)
                .orElse(false);
    }

    /**
     * Devuelve todas las solicitudes de moderación enviadas por el reporter
     * autenticado.
     *
     * @param auth usuario autenticado (reporter)
     * @return lista de solicitudes del reporter
     */
    @Transactional(readOnly = true)
    public List<ModeratorResponse> getMyRequests(Authentication auth) {
        Integer reporterId = getAuthenticatedUserId(auth);
        return moderatorReporterRepository.findByReporter_UserId(reporterId)
                .stream().map(ModeratorResponse::new).collect(Collectors.toList());
    }

    /**
     * Busca un usuario por email o nombre de usuario para enviarle una solicitud.
     * No permite que el reporter se busque a sí mismo.
     *
     * @param query término de búsqueda (email o username)
     * @param auth  usuario autenticado (reporter)
     * @return resumen del usuario encontrado
     */
    @Transactional(readOnly = true)
    public UserSummaryResponse searchUser(String query, Authentication auth) {
        Integer reporterId = getAuthenticatedUserId(auth);

        User user = userRepository.findByEmailOrUsername(query, query)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ningún usuario con el correo o usuario: " + query));

        if (user.getUserId().equals(reporterId))
            throw new BadRequestException("No puedes enviarte una solicitud a ti mismo");

        return mapToUserSummary(user);
    }

    // ------------------- REQUEST WORKFLOW ----------------------

    /**
     * Envía una solicitud de moderación a otro usuario.
     * Si ya existía una relación rechazada, la reactiva como pendiente.
     * Lanza excepción si ya hay una solicitud pendiente o aceptada.
     *
     * @param request datos de la solicitud (moderatorId destino)
     * @param auth    usuario autenticado (reporter)
     */
    @Transactional
    public void sendRequest(ModeratorRequest request, Authentication auth) {
        Integer reporterId = getAuthenticatedUserId(auth);
        Integer moderatorId = request.getModeratorId();

        if (reporterId.equals(moderatorId))
            throw new BadRequestException("No puedes enviarte una solicitud a ti mismo");

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado (reportero) no encontrado"));
        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario destino (moderador) no encontrado"));

        Optional<ModeratorReporter> existingRelation = moderatorReporterRepository
                .findByModerator_UserIdAndReporter_UserId(moderatorId, reporterId);

        if (existingRelation.isPresent()) {
            ModeratorReporter relation = existingRelation.get();
            if (relation.getStatus() == Status.PENDING) {
                throw new DuplicateResourceException("Ya existe una solicitud pendiente con este usuario.");
            } else if (relation.getStatus() == Status.ACCEPTED) {
                throw new DuplicateResourceException("Este usuario ya es tu moderador.");
            } else if (relation.getStatus() == Status.REJECTED) {
                relation.setStatus(Status.PENDING);
                moderatorReporterRepository.save(relation);
                return;
            }
        }

        ModeratorReporter moderatorReporter = new ModeratorReporter();
        moderatorReporter.setReporter(reporter);
        moderatorReporter.setModerator(moderator);
        moderatorReporter.setStatus(Status.PENDING);
        moderatorReporterRepository.save(moderatorReporter);
    }

    /**
     * Acepta una solicitud de moderación pendiente.
     * Si el usuario aceptante tenía rol READER, lo promueve a MODERATOR.
     *
     * @param requestId identificador de la solicitud
     * @param auth      usuario autenticado (destinatario de la solicitud)
     */
    @Transactional
    public void acceptRequest(Integer requestId, Authentication auth) {
        ModeratorReporter request = moderatorReporterRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con ID: " + requestId));

        Integer moderatorId = getAuthenticatedUserId(auth);
        if (!request.getModerator().getUserId().equals(moderatorId))
            throw new ForbiddenAccessException("No tienes permisos para aceptar esta solicitud");

        request.setStatus(Status.ACCEPTED);
        moderatorReporterRepository.save(request);

        User moderator = request.getModerator();
        if (moderator.getRole() == Role.READER) {
            moderator.setRole(Role.MODERATOR);
            userRepository.save(moderator);
        }
    }

    /**
     * Rechaza una solicitud de moderación pendiente.
     *
     * @param requestId identificador de la solicitud
     * @param auth      usuario autenticado (destinatario de la solicitud)
     */
    @Transactional
    public void rejectRequest(Integer requestId, Authentication auth) {
        ModeratorReporter request = moderatorReporterRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con ID: " + requestId));

        Integer moderatorId = getAuthenticatedUserId(auth);
        if (!request.getModerator().getUserId().equals(moderatorId))
            throw new ForbiddenAccessException("No tienes permisos para rechazar esta solicitud");

        request.setStatus(Status.REJECTED);
        moderatorReporterRepository.save(request);
    }

    /**
     * Cancela una solicitud de moderación enviada por el reporter autenticado.
     *
     * @param requestId identificador de la solicitud
     * @param auth      usuario autenticado (reporter que envió la solicitud)
     */
    @Transactional
    public void cancelRequest(Integer requestId, Authentication auth) {
        ModeratorReporter request = moderatorReporterRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con ID: " + requestId));

        Integer reporterId = getAuthenticatedUserId(auth);
        if (!request.getReporter().getUserId().equals(reporterId))
            throw new ForbiddenAccessException("No tienes permisos para cancelar esta solicitud");

        moderatorReporterRepository.delete(request);
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
     * Convierte una entidad {@link User} en su DTO de resumen.
     *
     * @param user entidad a convertir
     * @return DTO {@link UserSummaryResponse}
     */
    private UserSummaryResponse mapToUserSummary(User user) {
        UserSummaryResponse response = new UserSummaryResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        return response;
    }
}