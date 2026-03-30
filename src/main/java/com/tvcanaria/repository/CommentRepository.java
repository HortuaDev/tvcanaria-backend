package com.tvcanaria.repository;

import com.tvcanaria.entity.Comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    Page<Comment> findByArticle_ArticleIdOrderByCreatedAtDesc(Integer articleId, Pageable pageable);

    Page<Comment> findByOffenseCountGreaterThanEqual(Integer offenseCount, Pageable pageable);
}