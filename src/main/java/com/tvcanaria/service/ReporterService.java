package com.tvcanaria.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tvcanaria.dto.profile.ReporterProfileResponse;
import com.tvcanaria.entity.User;
import com.tvcanaria.entity.User.Role;
import com.tvcanaria.exception.ForbiddenAccessException;
import com.tvcanaria.exception.ResourceNotFoundException;
import com.tvcanaria.repository.UserRepository;

/**
 * Servicio para la consulta del perfil público de un reporter.
 */
@Service
public class ReporterService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Devuelve el perfil público de un reporter o administrador.
     * Lanza excepción si el usuario no existe o no tiene el rol adecuado.
     *
     * @param reporterId identificador del usuario
     * @return perfil público del reporter
     */
    public ReporterProfileResponse getReporter(Integer reporterId) {
        User user = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + reporterId));

        Role role = user.getRole();

        if (role != Role.REPORTER && role != Role.ADMIN) {
            throw new ForbiddenAccessException(
                    "El usuario no es reportero o administrador, no tiene un perfil público.");
        }

        return new ReporterProfileResponse(
                user.getUsername(), user.getFirstName(), user.getLastName(), user.getCreatedAt());
    }
}