package com.tvcanaria.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tvcanaria.dto.auth.AuthResponse;
import com.tvcanaria.dto.auth.GoogleTokenRequest;
import com.tvcanaria.dto.auth.LoginRequest;
import com.tvcanaria.dto.auth.RegisterRequest;
import com.tvcanaria.service.AuthService;
import com.tvcanaria.service.OAuth2Service;

/**
 * Controlador REST para autenticación y registro de usuarios.
 * Base path: /api/auth
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private OAuth2Service oAuth2Service;

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param request datos de registro (nombre, email, contraseña)
     * @return {@code 200 OK} con el token JWT y datos del usuario
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Autentica a un usuario con email y contraseña.
     *
     * @param request credenciales del usuario (email, contraseña)
     * @return {@code 200 OK} con el token JWT y datos del usuario
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Autentica a un usuario mediante un token de Google (OAuth2).
     * Si el usuario no existe, se registra automáticamente.
     *
     * @param request objeto con el {@code idToken} proporcionado por Google
     * @return {@code 200 OK} con el token JWT y datos del usuario
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleAuth(@Valid @RequestBody GoogleTokenRequest request) {
        AuthResponse response = oAuth2Service.authenticateGoogleToken(request.getIdToken());
        return ResponseEntity.ok(response);
    }

}