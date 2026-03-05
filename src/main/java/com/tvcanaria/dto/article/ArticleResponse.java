package com.tvcanaria.dto.article;

import java.math.BigDecimal;
import java.util.Set;

import com.tvcanaria.entity.Category;

public class ArticleResponse {
    private Integer id;
    private String title;
    private String description;
    private String videoUrl;
    private String location;
    private String authorUsername;
    private BigDecimal rating;
    private Set<Category> categories;

    public ArticleResponse(Integer id, String title, String description, String videoUrl, String location,
            String authorUsername, BigDecimal rating, Set<Category> categories) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
        this.location = location;
        this.authorUsername = authorUsername;
        this.rating = rating;
        this.categories = categories;
    }

    // Getters y setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
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