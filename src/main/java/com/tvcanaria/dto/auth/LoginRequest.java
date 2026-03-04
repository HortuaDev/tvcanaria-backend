package com.tvcanaria.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "El usuario o email es obligatorio")
    private String usernameOrEmail;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    // Getters y setters
    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
