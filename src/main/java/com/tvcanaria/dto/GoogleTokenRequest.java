package com.tvcanaria.dto;

import jakarta.validation.constraints.NotBlank;

public class GoogleTokenRequest {
    
    @NotBlank(message = "El idToken es requerido")
    private String idToken;

    public GoogleTokenRequest() {}

    public GoogleTokenRequest(String idToken) {
        this.idToken = idToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}