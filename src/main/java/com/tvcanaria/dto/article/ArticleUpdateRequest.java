package com.tvcanaria.dto.article;

import java.util.Set;

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
public class ArticleUpdateRequest {

    @Size(max = 150, message = "El título no puede superar 150 caracteres")
    private String title;

    @Size(max = 100, message = "La ubicación no puede superar 100 caracteres")
    private String location;

    private String description;

    private Set<Integer> categoryIds;

    private Boolean isHidden;
}