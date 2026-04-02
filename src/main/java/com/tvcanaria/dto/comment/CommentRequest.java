package com.tvcanaria.dto.comment;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

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
public class CommentRequest {

    @NotBlank(message = "El comentario no puede estar vacío")
    private String comment;

    @NotNull(message = "La valoración es obligatoria")
    @DecimalMin(value = "0.5", message = "La valoración mínima es 0.5")
    @DecimalMax(value = "5.0", message = "La valoración máxima es 5.0")
    private BigDecimal rating;

    @NotNull(message = "El ID del artículo es obligatorio")
    private Integer articleId;
}