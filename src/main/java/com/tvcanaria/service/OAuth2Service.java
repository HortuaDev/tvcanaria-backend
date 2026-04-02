package com.tvcanaria.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tvcanaria.dto.auth.AuthResponse;
import com.tvcanaria.entity.User;
import com.tvcanaria.enums.Role;
import com.tvcanaria.exception.AccountDisabledException;
import com.tvcanaria.exception.ExternalServiceException;
import com.tvcanaria.exception.InvalidCredentialsException;
import com.tvcanaria.repository.UserRepository;
import com.tvcanaria.security.JwtTokenProvider;

import java.util.Collections;

/**
 * Servicio para la autenticación de usuarios mediante Google OAuth2.
 * Soporta tanto el flujo web tradicional como el flujo móvil (idToken).
 */
@Service
public class OAuth2Service {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    /**
     * Procesa el login OAuth2 web tradicional a partir del usuario ya autenticado por Spring Security.
     * Crea el usuario si no existe. Rechaza el acceso si la cuenta está desactivada.
     *
     * @param oAuth2User usuario autenticado por Google (atributos: email, sub, given_name, family_name)
     * @return token JWT y datos básicos del usuario
     */
    @Transactional
    public AuthResponse processGoogleUser(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String googleId = oAuth2User.getAttribute("sub");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        User user = findOrCreateGoogleUser(email, googleId, firstName, lastName);

        if (!user.getIsActive()) {
            throw new AccountDisabledException("Tu cuenta ha sido desactivada. Contacta al administrador");
        }

        String token = jwtTokenProvider.generateToken(user);

        return new AuthResponse(token, user.getUserId(), user.getUsername(),
                user.getEmail(), user.getRole().name());
    }

    /**
     * Autentica a un usuario mediante un {@code idToken} de Google (flujo móvil / React Native).
     * Verifica la firma y audiencia del token antes de procesar al usuario.
     * Crea el usuario si no existe. Rechaza el acceso si la cuenta está desactivada.
     *
     * @param idTokenString token de identidad emitido por Google
     * @return token JWT y datos básicos del usuario
     */
    @Transactional
    public AuthResponse authenticateGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                throw new InvalidCredentialsException("Token de Google inválido, expirado o manipulado");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String googleId = payload.getSubject();
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");

            User user = findOrCreateGoogleUser(email, googleId, firstName, lastName);

            if (!user.getIsActive()) {
                throw new AccountDisabledException("Tu cuenta ha sido desactivada. Contacta al administrador");
            }

            String token = jwtTokenProvider.generateToken(user);

            return new AuthResponse(token, user.getUserId(), user.getUsername(),
                    user.getEmail(), user.getRole().name());

        } catch (InvalidCredentialsException | AccountDisabledException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException(
                    "Error conectando con los servidores de Google para validar la identidad");
        }
    }

    /**
     * Busca un usuario por {@code googleId} o email; si no existe, lo crea con rol READER.
     * Si ya existe por email con otro proveedor, actualiza sus datos de Google.
     *
     * @param email     email del usuario
     * @param googleId  identificador único de Google (subject)
     * @param firstName nombre de pila
     * @param lastName  apellidos
     * @return entidad {@link User} encontrada o creada
     */
    private User findOrCreateGoogleUser(String email, String googleId, String firstName, String lastName) {
        return userRepository.findByProviderId(googleId)
                .orElseGet(() -> {
                    return userRepository.findByEmail(email)
                            .map(existingUser -> {
                                existingUser.setAuthProvider("GOOGLE");
                                existingUser.setProviderId(googleId);
                                return userRepository.save(existingUser);
                            })
                            .orElseGet(() -> {
                                User newUser = new User();
                                newUser.setEmail(email);
                                newUser.setUsername(generateUsername(email));
                                newUser.setFirstName(firstName != null ? firstName : "");
                                newUser.setLastName(lastName != null ? lastName : "");
                                newUser.setAuthProvider("GOOGLE");
                                newUser.setProviderId(googleId);
                                newUser.setPasswordHash("OAUTH2_USER");
                                newUser.setRole(Role.READER);
                                newUser.setIsActive(true);
                                return userRepository.save(newUser);
                            });
                });
    }

    /**
     * Genera un nombre de usuario único a partir del prefijo del email.
     * Añade un sufijo numérico incremental si el username ya está en uso.
     *
     * @param email email del usuario
     * @return nombre de usuario único
     */
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