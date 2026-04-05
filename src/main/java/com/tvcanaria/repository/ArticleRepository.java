package com.tvcanaria.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        Page<Article> findAll(Pageable pageable);

        @EntityGraph(attributePaths = { "author", "categories" })
        Page<Article> findByIsHiddenFalse(Pageable pageable);

        Page<Article> findByCategoriesCategoryId(Integer categoryId, Pageable pageable);

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

        @EntityGraph(attributePaths = { "author", "categories" })
        @Query("SELECT DISTINCT a FROM Article a JOIN a.categories c " +
                        "WHERE c IN :categories AND a.articleId != :articleId AND a.isHidden = false " +
                        "ORDER BY a.createdAt DESC, COALESCE(a.rating, 0) DESC")
        List<Article> findRelatedArticles(@Param("categories") Set<Category> categories,
                        @Param("articleId") Integer articleId,
                        Pageable pageable);

        @EntityGraph(attributePaths = { "author", "categories" })
        @Query("SELECT a FROM Article a " +
                        "WHERE a.articleId != :articleId AND a.isHidden = false " +
                        "ORDER BY a.createdAt DESC, COALESCE(a.rating, 0) DESC")
        List<Article> findFallbackRelatedArticles(@Param("articleId") Integer articleId,
                        Pageable pageable);

        @EntityGraph(attributePaths = { "author", "categories" })
        @Query("SELECT a FROM Article a " +
                        "WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "AND a.isHidden = false " +
                        "ORDER BY a.createdAt DESC, COALESCE(a.rating, 0) DESC")
        List<Article> searchVisibleArticlesByTitle(@Param("keyword") String keyword);

        List<Article> findByAuthorUserId(Integer userId);

        @EntityGraph(attributePaths = { "author", "categories" })
        @Query("SELECT DISTINCT a FROM Article a LEFT JOIN a.categories c WHERE " +
                        "(:role = 'ADMIN' OR a.author.userId = :userId) AND " +
                        "(CAST(:dateFrom AS timestamp) IS NULL OR a.createdAt >= :dateFrom) AND " +
                        "(CAST(:dateTo AS timestamp) IS NULL OR a.createdAt <= :dateTo) AND " +
                        "(:hasCategories = false OR c.name IN :categories) AND " +
                        "(:keyword IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<Article> findMyArticlesWithFilters(
                        @Param("userId") Integer userId,
                        @Param("role") String role,
                        @Param("dateFrom") LocalDateTime dateFrom,
                        @Param("dateTo") LocalDateTime dateTo,
                        @Param("hasCategories") boolean hasCategories,
                        @Param("categories") List<String> categories,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @EntityGraph(attributePaths = { "author", "categories" })
        @Query("SELECT DISTINCT a FROM Article a LEFT JOIN a.categories c WHERE " +
                        "a.isHidden = false AND " +
                        "(:categoryId IS NULL OR c.categoryId = :categoryId) AND " +
                        "(CAST(:dateFrom AS timestamp) IS NULL OR a.createdAt >= :dateFrom) AND " +
                        "(CAST(:dateTo AS timestamp) IS NULL OR a.createdAt <= :dateTo) " +
                        "ORDER BY a.createdAt DESC")
        Page<Article> findFeedArticlesWithFilters(
                        @Param("categoryId") Integer categoryId,
                        @Param("dateFrom") LocalDateTime dateFrom,
                        @Param("dateTo") LocalDateTime dateTo,
                        Pageable pageable);
}
