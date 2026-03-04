package com.tvcanaria.dto.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ArticleRequest {
    @NotBlank
    @Size(max = 150)
    private String title;

    private String description;

    private String video_url;

    private String location;

    // Getters y setters
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

    public String getVideo_url() {
        return video_url;
    }

    public void setVideo_url(String video_url) {
        this.video_url = video_url;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
