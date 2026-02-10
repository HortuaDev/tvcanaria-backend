package com.tvcanaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tvcanaria.entity.Article;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Integer> {
    // Aqui se agregan las consultas personalizadas cunado las necesitemos

    // Ejmp: List<Article> findByTitleContaining(String keyword);
}
