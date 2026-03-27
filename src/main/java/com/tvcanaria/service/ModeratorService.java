package com.tvcanaria.service;

import com.tvcanaria.dto.moderator.ModeratorRequest;
import com.tvcanaria.dto.moderator.ModeratorResponse;
import com.tvcanaria.dto.user.UserSummaryResponse;
import com.tvcanaria.entity.ModeratorReporter;
import com.tvcanaria.entity.User;
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
                .orElseThrow(() -> new RuntimeException("Moderador no encontrado"));

        if (moderator.getRole() != User.Role.MODERATOR && moderator.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("El usuario no es un moderador");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Reportero no encontrado"));

        if (reporter.getRole() != User.Role.REPORTER) {
            throw new RuntimeException("El usuario no es un reportero");
        }

        moderatorReporterRepository
                .findByModerator_UserIdAndReporter_UserId(moderatorId, reporterId)
                .ifPresent(r -> {
                    throw new RuntimeException("Ya existe una relación entre estos usuarios");
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
                .orElseThrow(() -> new RuntimeException("Relación no encontrada"));

        moderatorReporterRepository.delete(relation);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getReportersByModerator(Integer moderatorId) {
        userRepository.findById(moderatorId)
                .orElseThrow(() -> new RuntimeException("Moderador no encontrado"));

        return moderatorReporterRepository
                .findAcceptedReportersByModerator(moderatorId, ModeratorReporter.Status.ACCEPTED)
                .stream()
                .map(this::mapToUserSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getModeratorsByReporter(Integer reporterId) {
        userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Reportero no encontrado"));

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
                    throw new RuntimeException("Ya existe una solicitud entre estos usuarios");
                });

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User moderator = userRepository.findById(request.getModeratorId())
                .orElseThrow(() -> new RuntimeException("User not found"));

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
                .orElseThrow(() -> new RuntimeException("Request not found"));

        Integer moderatorId = Integer.valueOf(auth.getName());
        if (!request.getModerator().getUserId().equals(moderatorId)) {
            throw new RuntimeException("No tiene permisos para aceptar esta solicitud");
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
                .orElseThrow(() -> new RuntimeException("Request not found"));

        Integer moderatorId = Integer.valueOf(auth.getName());
        if (!request.getModerator().getUserId().equals(moderatorId)) {
            throw new RuntimeException("No tiene permisos para rechazar esta solicitud");
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
        System.out.println(email);
        Integer reporterId = Integer.valueOf(auth.getName());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getUserId().equals(reporterId)) {
            throw new RuntimeException("No puedes enviarte una solicitud a ti mismo");
        }

        return mapToUserSummary(user);
    }

    @Transactional
    public void cancelRequest(Integer requestId, Authentication auth) {
        ModeratorReporter request = moderatorReporterRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        Integer reporterId = Integer.valueOf(auth.getName());
        if (!request.getReporter().getUserId().equals(reporterId)) {
            throw new RuntimeException("No tienes permisos para cancelar esta solicitud");
        }

        moderatorReporterRepository.delete(request);
    }
}