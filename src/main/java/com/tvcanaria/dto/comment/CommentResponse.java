package com.tvcanaria.dto.comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
public class CommentResponse {

    private Integer commentId;

    private String comment;

    private LocalDateTime createdAt;

    private Integer offenseCount;

    private BigDecimal rating;

    private String username;

    private Integer authorId;

    private Integer articleId;
}