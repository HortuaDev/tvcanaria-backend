package com.tvcanaria.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tvcanaria.dto.auth.AuthResponse;
import com.tvcanaria.dto.auth.LoginRequest;
import com.tvcanaria.dto.auth.RegisterRequest;
import com.tvcanaria.dto.profile.UpdateProfileRequest;
import com.tvcanaria.dto.profile.UserProfileResponse;
import com.tvcanaria.entity.User;
import com.tvcanaria.exception.*;
import com.tvcanaria.repository.UserRepository;
import com.tvcanaria.security.JwtTokenProvider;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validar si el usuario ya existe
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("El email ya está registrado");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("El nombre de usuario ya está en uso");
        }

        // Crear nuevo usuario
        User user = new User();
        user.setUsername(request.getUsername());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.READER);
        user.setAuthProvider("LOCAL");
        user.setIsActive(true);

        user = userRepository.save(user);

        // Generar token JWT
        String token = jwtTokenProvider.generateToken(user);

        return new AuthResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.getUsernameOrEmail();

        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new InvalidCredentialsException("Email/usuario o contraseña incorrectos"));

        if (!user.getIsActive()) {
            throw new AccountDisabledException("Tu cuenta ha sido desactivada. Contacta al administrador");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Email/usuario o contraseña incorrectos");
        }

        String token = jwtTokenProvider.generateToken(user);

        return new AuthResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name());
    }

    public UserProfileResponse getUserProfile(String identifier) {
        try {
            // El identifier es el userId en formato String
            Integer userId = Integer.parseInt(identifier);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            return new UserProfileResponse(user);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Identificador de usuario inválido");
        }
    }

    public UserProfileResponse updateUserProfile(String identifier, UpdateProfileRequest request) {
        try {
            Integer userId = Integer.parseInt(identifier);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Actualizar nombre
            if (request.getFirstName() != null) {
                user.setFirstName(request.getFirstName());
            }

            // Actualizar apellidos
            if (request.getLastName() != null) {
                user.setLastName(request.getLastName());
            }

            // Actualizar email si es diferente
            if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
                if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                    throw new RuntimeException("El correo electrónico ya está en uso");
                }
                user.setEmail(request.getEmail());
            }

            User updatedUser = userRepository.save(user);
            return new UserProfileResponse(updatedUser);

        } catch (NumberFormatException e) {
            throw new RuntimeException("Identificador de usuario inválido");
        }
    }
}