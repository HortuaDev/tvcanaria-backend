package com.tvcanaria.dto.article;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import com.tvcanaria.entity.Article;
import com.tvcanaria.entity.Category;

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
    private Set<Category> categories;

    public ArticleResponse(Integer id, String title, String description, String videoUrl, String location,
            String authorUsername, BigDecimal rating, Set<Category> categories) {
        this.articleId = id;
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
        this.location = location;
        this.authorUsername = authorUsername;
        this.rating = rating;
        this.categories = categories;
    }

    // Constructor que mapea un Article a ArticleResponse
    public ArticleResponse(Article article) {
        this.articleId = article.getArticleId();
        this.title = article.getTitle();
        this.description = article.getDescription();
        this.videoUrl = article.getVideoUrl();
        this.location = article.getLocation();
        this.authorUsername = article.getAuthor().getUsername();
        this.rating = article.getRating();
        this.categories = article.getCategories();

    }

    // Getters

    public ArticleResponse(Integer articleId2, String title2, String description2, String videoUrl2, String location2,
            String authentication) {
    }

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

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public void setCategories(Set<Category> categories) {
        this.categories = categories;
    }

}