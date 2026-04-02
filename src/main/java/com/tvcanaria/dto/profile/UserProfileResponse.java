package com.tvcanaria.dto.profile;

import com.tvcanaria.entity.User;
import com.tvcanaria.enums.Role;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Integer userId;

    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private String role;

    private Boolean isActive;

    private String authProvider;

    private LocalDateTime createdAt;

    public UserProfileResponse(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.role = translateRole(user.getRole());
        this.isActive = user.getIsActive();
        this.authProvider = user.getAuthProvider();
        this.createdAt = user.getCreatedAt();
    }

    private String translateRole(Role role) {
        return switch (role) {
            case READER -> "Lector";
            case REPORTER -> "Reportero";
            case MODERATOR -> "Moderador";
            case ADMIN -> "Administrador";
        };
    }
}