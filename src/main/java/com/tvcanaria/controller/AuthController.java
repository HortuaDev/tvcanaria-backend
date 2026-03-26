package com.tvcanaria.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tvcanaria.dto.auth.AuthResponse;
import com.tvcanaria.dto.auth.GoogleTokenRequest;
import com.tvcanaria.dto.auth.LoginRequest;
import com.tvcanaria.dto.auth.RegisterRequest;
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
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleAuth(@Valid @RequestBody GoogleTokenRequest request) {
        AuthResponse response = oAuth2Service.authenticateGoogleToken(request.getIdToken());
        return ResponseEntity.ok(response);
    }

}