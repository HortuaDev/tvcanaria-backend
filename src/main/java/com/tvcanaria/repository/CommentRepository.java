package com.tvcanaria.repository;

import com.tvcanaria.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    List<Comment> findByArticle_ArticleIdOrderByCreatedAtDesc(Integer articleId);
    
    List<Comment> findByOffenseCountGreaterThanEqual(Integer offenseCount);
}