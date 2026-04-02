package com.tvcanaria.dto.profile;

import com.tvcanaria.entity.User;
import com.tvcanaria.enums.Role;

import java.time.LocalDateTime;

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

    // Getters y setters
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}