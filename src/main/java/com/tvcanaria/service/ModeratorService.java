package com.tvcanaria.service;

import com.tvcanaria.dto.user.UserSummaryResponse;
import com.tvcanaria.entity.User;
import com.tvcanaria.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModeratorService {
    @Autowired
    private UserRepository userRepository;

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

        moderator.getAssignedReporters().add(reporter);
        userRepository.save(moderator);
    }

    @Transactional
    public void removeReporterFromModerator(Integer moderatorId, Integer reporterId) {
        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new RuntimeException("Moderador no encontrado"));

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Reportero no encontrado"));

        moderator.getAssignedReporters().remove(reporter);
        userRepository.save(moderator);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getReportersByModerator(Integer moderatorId) {
        userRepository.findById(moderatorId)
                .orElseThrow(() -> new RuntimeException("Moderador no encontrado"));

        List<User> reporters = userRepository.findReportersByModerator(moderatorId);
        return reporters.stream()
                .map(this::mapToUserSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getModeratorsByReporter(Integer reporterId) {
        userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Reportero no encontrado"));

        List<User> moderators = userRepository.findModeratorsByReporter(reporterId);
        return moderators.stream()
                .map(this::mapToUserSummary)
                .collect(Collectors.toList());
    }

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