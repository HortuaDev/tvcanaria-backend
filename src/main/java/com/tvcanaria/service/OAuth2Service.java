package com.tvcanaria.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tvcanaria.dto.auth.AuthResponse;
import com.tvcanaria.entity.User;
import com.tvcanaria.repository.UserRepository;
import com.tvcanaria.security.JwtTokenProvider;

import java.util.Collections;

@Service
public class OAuth2Service {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    public OAuth2Service(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Método para manejar login desde OAuth2 web tradicional (usado por el handler)
     */
    @Transactional
    public AuthResponse processGoogleUser(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String googleId = oAuth2User.getAttribute("sub");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        User user = findOrCreateGoogleUser(email, googleId, firstName, lastName);
        String token = jwtTokenProvider.generateToken(user);

        return new AuthResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name());
    }

    /**
     * Método para manejar login desde React Native/Expo (recibe idToken)
     */
    @Transactional
    public AuthResponse authenticateGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                throw new RuntimeException("Token de Google inválido");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            String googleId = payload.getSubject();
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");

            User user = findOrCreateGoogleUser(email, googleId, firstName, lastName);
            String token = jwtTokenProvider.generateToken(user);

            return new AuthResponse(
                    token,
                    user.getUserId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().name());

        } catch (Exception e) {
            throw new RuntimeException("Error validando token de Google: " + e.getMessage());
        }
    }

    /**
     * Método privado compartido para buscar o crear usuario de Google
     */
    private User findOrCreateGoogleUser(String email, String googleId, String firstName, String lastName) {
        return userRepository.findByProviderId(googleId)
                .orElseGet(() -> {
                    return userRepository.findByEmail(email)
                            .map(existingUser -> {
                                // Vincular cuenta existente con Google
                                existingUser.setAuthProvider("GOOGLE");
                                existingUser.setProviderId(googleId);
                                return userRepository.save(existingUser);
                            })
                            .orElseGet(() -> {
                                // Crear nuevo usuario
                                User newUser = new User();
                                newUser.setEmail(email);
                                newUser.setUsername(generateUsername(email));
                                newUser.setFirstName(firstName != null ? firstName : "");
                                newUser.setLastName(lastName != null ? lastName : "");
                                newUser.setAuthProvider("GOOGLE");
                                newUser.setProviderId(googleId);
                                newUser.setPasswordHash("OAUTH2_USER");
                                newUser.setRole(User.Role.READER);
                                newUser.setIsActive(true);
                                return userRepository.save(newUser);
                            });
                });
    }

    private String generateUsername(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}