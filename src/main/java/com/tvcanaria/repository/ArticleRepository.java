package com.tvcanaria.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tvcanaria.entity.Article;
import com.tvcanaria.entity.Category;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Integer> {

  @EntityGraph(attributePaths = { "author", "categories" })
  List<Article> findAll();

  @EntityGraph(attributePaths = { "author", "categories" })
  List<Article> findByIsHiddenFalse();

  List<Article> findByCategoriesCategoryId(Integer categoryId);

  @Query(value = """
      SELECT COALESCE(ROUND(AVG(ultimos_votos.rating) * 2) / 2, 0)
      FROM (
          SELECT rating
          FROM comment c
          WHERE c.article_id = :articleId
            AND c.rating >= 0.5
            AND c.created_at = (
                SELECT MAX(inner_c.created_at)
                FROM comment inner_c
                WHERE inner_c.user_id = c.user_id
                  AND inner_c.article_id = :articleId
            )
      ) AS ultimos_votos
      """, nativeQuery = true)
  BigDecimal calculateAverageByArticleId(@Param("articleId") Integer articleId);

  @EntityGraph(attributePaths = { "author", "categories" })
  List<Article> findTop20DistinctByCategoriesInAndIsHiddenFalseOrderByCreatedAtDesc(Set<Category> categories);

  @EntityGraph(attributePaths = { "author", "categories" })
  List<Article> findTop20ByIsHiddenFalseOrderByCreatedAtDesc();

  List<Article> findByAuthorUserId(Integer userId);
}
