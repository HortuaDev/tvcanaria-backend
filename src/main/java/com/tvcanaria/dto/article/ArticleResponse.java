package com.tvcanaria.dto.article;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import com.tvcanaria.dto.category.CategoryResponse;
import com.tvcanaria.entity.Article;

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
public class ArticleResponse {

    private Integer articleId;
    private String title;
    private String description;
    private String videoUrl;
    private Boolean isHidden;
    private String location;
    private LocalDateTime createdAt;

    private Integer authorId;
    private String authorUsername;
    private BigDecimal rating;

    private Set<CategoryResponse> categories;

    public ArticleResponse(Article article) {

        this.articleId = article.getArticleId();
        this.title = article.getTitle();
        this.description = article.getDescription();
        this.videoUrl = article.getVideoUrl();
        this.isHidden = article.getIsHidden();
        this.location = article.getLocation();
        this.createdAt = article.getCreatedAt();

        this.authorId = article.getAuthor().getUserId();
        this.authorUsername = article.getAuthor().getUsername();
        this.rating = article.getRating();

        this.categories = article.getCategories()
                .stream()
                .map(CategoryResponse::new)
                .collect(Collectors.toSet());
    }

}