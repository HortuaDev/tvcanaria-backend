package com.tvcanaria.service;

import com.tvcanaria.dto.moderator.ModeratorRequest;
import com.tvcanaria.dto.moderator.ModeratorResponse;
import com.tvcanaria.dto.user.UserSummaryResponse;
import com.tvcanaria.entity.ModeratorReporter;
import com.tvcanaria.entity.User;
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
import java.util.stream.Collectors;

@Service
public class ModeratorService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModeratorReporterRepository moderatorReporterRepository;

    @Transactional
    public void assignReporterToModerator(Integer moderatorId, Integer reporterId) {
        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Moderador no encontrado con ID: " + moderatorId));

        if (moderator.getRole() != User.Role.MODERATOR && moderator.getRole() != User.Role.ADMIN) {
            throw new BadRequestException("El usuario seleccionado no tiene el rol de moderador");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Reportero no encontrado con ID: " + reporterId));

        if (reporter.getRole() != User.Role.REPORTER) {
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
        relation.setStatus(ModeratorReporter.Status.ACCEPTED);
        moderatorReporterRepository.save(relation);
    }

    @Transactional
    public void removeReporterFromModerator(Integer moderatorId, Integer reporterId) {
        ModeratorReporter relation = moderatorReporterRepository
                .findByModerator_UserIdAndReporter_UserId(moderatorId, reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Relación no encontrada entre estos usuarios"));

        moderatorReporterRepository.delete(relation);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getReportersByModerator(Integer moderatorId) {
        if (!userRepository.existsById(moderatorId)) {
            throw new ResourceNotFoundException("Moderador no encontrado con ID: " + moderatorId);
        }

        return moderatorReporterRepository
                .findAcceptedReportersByModerator(moderatorId, ModeratorReporter.Status.ACCEPTED)
                .stream()
                .map(this::mapToUserSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getModeratorsByReporter(Integer reporterId) {
        if (!userRepository.existsById(reporterId)) {
            throw new ResourceNotFoundException("Reportero no encontrado con ID: " + reporterId);
        }

        return moderatorReporterRepository
                .findAcceptedModeratorsByReporter(reporterId, ModeratorReporter.Status.ACCEPTED)
                .stream()
                .map(this::mapToUserSummary)
                .collect(Collectors.toList());
    }

    @Transactional
    public void sendRequest(ModeratorRequest request, Authentication auth) {
        Integer reporterId = Integer.valueOf(auth.getName());

        moderatorReporterRepository
                .findByModerator_UserIdAndReporter_UserId(request.getModeratorId(), reporterId)
                .ifPresent(r -> {
                    throw new DuplicateResourceException("Ya existe una solicitud previa entre estos usuarios");
                });

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado (reportero) no encontrado"));

        User moderator = userRepository.findById(request.getModeratorId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario destino (moderador) no encontrado"));

        ModeratorReporter moderatorReporter = new ModeratorReporter();
        moderatorReporter.setReporter(reporter);
        moderatorReporter.setModerator(moderator);
        moderatorReporter.setStatus(ModeratorReporter.Status.PENDING);

        moderatorReporterRepository.save(moderatorReporter);
    }

    // Usuario obtiene sus solicitudes pendientes
    @Transactional(readOnly = true)
    public List<ModeratorResponse> getPendingRequests(Authentication auth) {
        Integer moderatorId = Integer.valueOf(auth.getName());
        return moderatorReporterRepository
                .findByModerator_UserIdAndStatus(moderatorId, ModeratorReporter.Status.PENDING)
                .stream()
                .map(ModeratorResponse::new)
                .collect(Collectors.toList());
    }

    // Usuario acepta la solicitud
    @Transactional
    public void acceptRequest(Integer requestId, Authentication auth) {
        ModeratorReporter request = moderatorReporterRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con ID: " + requestId));

        Integer moderatorId = Integer.valueOf(auth.getName());
        if (!request.getModerator().getUserId().equals(moderatorId)) {
            throw new ForbiddenAccessException("No tienes permisos para aceptar esta solicitud");
        }

        request.setStatus(ModeratorReporter.Status.ACCEPTED);
        moderatorReporterRepository.save(request);

        // Cambiar el rol del usuario a MODERATOR
        User moderator = request.getModerator();
        if (moderator.getRole() == User.Role.READER) {
            moderator.setRole(User.Role.MODERATOR);
            userRepository.save(moderator);
        }
    }

    // Usuario rechaza la solicitud
    @Transactional
    public void rejectRequest(Integer requestId, Authentication auth) {
        ModeratorReporter request = moderatorReporterRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con ID: " + requestId));

        Integer moderatorId = Integer.valueOf(auth.getName());
        if (!request.getModerator().getUserId().equals(moderatorId)) {
            throw new ForbiddenAccessException("No tienes permisos para rechazar esta solicitud");
        }

        request.setStatus(ModeratorReporter.Status.REJECTED);
        moderatorReporterRepository.save(request);
    }

    // ---- Helper ----

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

    public boolean isModeratorOf(Integer moderatorId, Integer reporterId) {
        return moderatorReporterRepository
                .findByModerator_UserIdAndReporter_UserId(moderatorId, reporterId)
                .map(r -> r.getStatus() == ModeratorReporter.Status.ACCEPTED)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<ModeratorResponse> getMyRequests(Authentication auth) {
        Integer reporterId = Integer.valueOf(auth.getName());
        return moderatorReporterRepository.findByReporter_UserId(reporterId)
                .stream()
                .map(ModeratorResponse::new)
                .collect(Collectors.toList());
    }

    public UserSummaryResponse searchUserByEmail(String email, Authentication auth) {
        Integer reporterId = Integer.valueOf(auth.getName());

        User user = userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No se encontró ningún usuario con el email: " + email));

        if (user.getUserId().equals(reporterId)) {
            throw new BadRequestException("No puedes enviarte una solicitud a ti mismo");
        }

        return mapToUserSummary(user);
    }

    @Transactional
    public void cancelRequest(Integer requestId, Authentication auth) {
        ModeratorReporter request = moderatorReporterRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con ID: " + requestId));

        Integer reporterId = Integer.valueOf(auth.getName());
        if (!request.getReporter().getUserId().equals(reporterId)) {
            throw new ForbiddenAccessException("No tienes permisos para cancelar esta solicitud");
        }

        moderatorReporterRepository.delete(request);
    }
}