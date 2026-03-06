package com.tvcanaria.dto.article;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import com.tvcanaria.entity.Article;

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

    private Set<String> categories;

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
                .map(category -> category.getName())
                .collect(Collectors.toSet());

    }

    // Getters

    public Integer getArticleId() {
        return articleId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public Boolean getIsHidden() {
        return isHidden;
    }

    public String getLocation() {
        return location;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

}