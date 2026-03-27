package com.tvcanaria.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tvcanaria.dto.profile.ReporterProfileResponse;
import com.tvcanaria.entity.User;
import com.tvcanaria.entity.User.Role;
import com.tvcanaria.repository.UserRepository;

@Service
public class ReporterService {

    @Autowired
    private UserRepository userRepository;

    public ReporterProfileResponse getReporter(Integer reporterId) {

        User user = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Role role = user.getRole();

        if (role != Role.REPORTER || role != Role.ADMIN) {
            throw new RuntimeException("El usuario no tiene permisos de reportero o administrador");
        }

        return new ReporterProfileResponse(user.getUsername(), user.getFirstName(), user.getLastName(), user.getCreatedAt());
    }
}