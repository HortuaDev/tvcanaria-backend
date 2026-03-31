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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ModeratorService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModeratorReporterRepository moderatorReporterRepository;

    @Transactional
    public void assignReporterToModerator(Integer moderatorId, Integer reporterId) {
        // ... (Tu código actual de este método se mantiene igual)
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
                .stream().map(this::mapToUserSummary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getModeratorsByReporter(Integer reporterId) {
        if (!userRepository.existsById(reporterId)) {
            throw new ResourceNotFoundException("Reportero no encontrado con ID: " + reporterId);
        }
        return moderatorReporterRepository
                .findAcceptedModeratorsByReporter(reporterId, ModeratorReporter.Status.ACCEPTED)
                .stream().map(this::mapToUserSummary).collect(Collectors.toList());
    }

    @Transactional
    public void sendRequest(ModeratorRequest request, Authentication auth) {
        Integer reporterId = Integer.valueOf(auth.getName());
        Integer moderatorId = request.getModeratorId();

        if (reporterId.equals(moderatorId)) {
            throw new BadRequestException("No puedes enviarte una solicitud a ti mismo");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado (reportero) no encontrado"));

        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario destino (moderador) no encontrado"));

        Optional<ModeratorReporter> existingRelation = moderatorReporterRepository
                .findByModerator_UserIdAndReporter_UserId(moderatorId, reporterId);

        // MEJORA: Validar estados específicos para evitar bloqueos eternos si fue
        // rechazada antes.
        if (existingRelation.isPresent()) {
            ModeratorReporter relation = existingRelation.get();
            if (relation.getStatus() == ModeratorReporter.Status.PENDING) {
                throw new DuplicateResourceException("Ya existe una solicitud pendiente con este usuario.");
            } else if (relation.getStatus() == ModeratorReporter.Status.ACCEPTED) {
                throw new DuplicateResourceException("Este usuario ya es tu moderador.");
            } else if (relation.getStatus() == ModeratorReporter.Status.REJECTED) {
                // Si estaba rechazada, le damos otra oportunidad y la pasamos a PENDING
                relation.setStatus(ModeratorReporter.Status.PENDING);
                moderatorReporterRepository.save(relation);
                return;
            }
        }

        ModeratorReporter moderatorReporter = new ModeratorReporter();
        moderatorReporter.setReporter(reporter);
        moderatorReporter.setModerator(moderator);
        moderatorReporter.setStatus(ModeratorReporter.Status.PENDING);
        moderatorReporterRepository.save(moderatorReporter);
    }

    @Transactional(readOnly = true)
    public List<ModeratorResponse> getPendingRequests(Authentication auth) {
        Integer moderatorId = Integer.valueOf(auth.getName());
        return moderatorReporterRepository
                .findByModerator_UserIdAndStatus(moderatorId, ModeratorReporter.Status.PENDING)
                .stream().map(ModeratorResponse::new).collect(Collectors.toList());
    }

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

        User moderator = request.getModerator();
        if (moderator.getRole() == User.Role.READER) {
            moderator.setRole(User.Role.MODERATOR);
            userRepository.save(moderator);
        }
    }

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

    @Transactional(readOnly = true)
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
                .stream().map(ModeratorResponse::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse searchUser(String query, Authentication auth) {
        Integer reporterId = Integer.valueOf(auth.getName());

        User user = userRepository.findByEmailOrUsername(query, query)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ningún usuario con el correo o usuario: " + query));

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
}