package com.tvcanaria.dto.profile;

import java.time.LocalDateTime;

public class ReporterProfileResponse {
    private String username;
    private String firstName;
    private String lastName;
    private LocalDateTime createdAt;

    public ReporterProfileResponse(String username, String firstName, String lastName, LocalDateTime createdAt) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = createdAt;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
