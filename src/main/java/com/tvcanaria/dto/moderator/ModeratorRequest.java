package com.tvcanaria.dto.moderator;

import jakarta.validation.constraints.NotNull;

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
public class ModeratorRequest {

    @NotNull(message = "El ID del moderador es obligatorio")
    private Integer moderatorId;
}