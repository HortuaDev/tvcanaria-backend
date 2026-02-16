package com.tvcanaria.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tvcanaria.dto.*;
import com.tvcanaria.service.AuthService;
import com.tvcanaria.service.OAuth2Service;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuth2Service oAuth2Service;

    public AuthController(AuthService authService, OAuth2Service oAuth2Service) {
        this.authService = authService;
        this.oAuth2Service = oAuth2Service;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    // Endpoint para autenticación con Google (recibe idToken del frontend)
    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@Valid @RequestBody GoogleTokenRequest request) {
        try {
            AuthResponse response = oAuth2Service.authenticateGoogleToken(request.getIdToken());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(e.getMessage()));
        }
    }
}