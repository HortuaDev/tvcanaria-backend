package com.tvcanaria.dto.article;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
public class ArticleRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    private String title;

    private String description;

    @Size(max = 255, message = "La URL del video es demasiado larga")
    private String videoUrl;

    @Size(max = 100, message = "La localización es demasiado larga")
    private String location;

    @NotNull(message = "La lista de categorías no puede ser nula")
    @NotEmpty(message = "Debes asignar al menos una categoría")
    private Set<Integer> categories;
}