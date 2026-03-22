package com.tvcanaria.dto.moderator;

import jakarta.validation.constraints.NotNull;

public class ModeratorRequest {

    @NotNull(message = "El ID del moderador es obligatorio")
    private Integer moderatorId;

    public Integer getModeratorId() {
        return moderatorId;
    }

    public void setModeratorId(Integer moderatorId) {
        this.moderatorId = moderatorId;
    }
}