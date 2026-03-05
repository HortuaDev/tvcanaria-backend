package com.tvcanaria.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tvcanaria.entity.Article;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Integer> {

    List<Article> findByIsHiddenFalse();

    List<Article> findByCategoriesCategoryId(Integer categoryId);

    @Query(value = "SELECT COALESCE(ROUND(AVG(rating) * 2) / 2, 0) FROM comment WHERE article_id = :articleId", nativeQuery = true)
    BigDecimal calculateAverageByArticleId(@Param("articleId") Integer articleId);
}
