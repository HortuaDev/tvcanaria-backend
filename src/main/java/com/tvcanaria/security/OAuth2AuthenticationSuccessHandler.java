package com.tvcanaria.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import com.tvcanaria.dto.AuthResponse;
import com.tvcanaria.service.OAuth2Service;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2Service oAuth2Service;

    public OAuth2AuthenticationSuccessHandler(OAuth2Service oAuth2Service) {
        this.oAuth2Service = oAuth2Service;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        // Procesar usuario de Google y generar JWT
        AuthResponse authResponse = oAuth2Service.processGoogleUser(oAuth2User);

        // Redirigir al frontend con el token
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/auth/callback")
                .queryParam("token", authResponse.getToken())
                .queryParam("userId", authResponse.getUserId())
                .queryParam("username", authResponse.getUsername())
                .queryParam("email", authResponse.getEmail())
                .queryParam("role", authResponse.getRole())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}