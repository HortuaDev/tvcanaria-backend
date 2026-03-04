package com.tvcanaria.dto.comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CommentResponse {

    private Integer commentId;
    private String comment;
    private LocalDateTime createdAt;
    private Integer offenseCount;
    private BigDecimal rating;
    private String username;

    public Integer getCommentId() {
        return commentId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getOffenseCount() {
        return offenseCount;
    }

    public void setOffenseCount(Integer offenseCount) {
        this.offenseCount = offenseCount;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}