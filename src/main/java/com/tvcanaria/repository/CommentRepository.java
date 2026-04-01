package com.tvcanaria.repository;

import com.tvcanaria.entity.Comment;
import com.tvcanaria.entity.ModeratorReporter;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
        Page<Comment> findByArticle_ArticleIdOrderByCreatedAtDesc(Integer articleId, Pageable pageable);

        @Query("SELECT c FROM Comment c WHERE c.offenseCount >= 1 " +
                        "AND (CAST(:dateFrom AS timestamp) IS NULL OR c.createdAt >= :dateFrom) " +
                        "AND (CAST(:dateTo AS timestamp) IS NULL OR c.createdAt <= :dateTo)")
        Page<Comment> findReportedCommentsWithFilters(
                        @Param("dateFrom") LocalDateTime dateFrom,
                        @Param("dateTo") LocalDateTime dateTo,
                        Pageable pageable);

        @Query("SELECT c FROM Comment c " +
                        "JOIN c.article a " +
                        "JOIN a.author reporter " +
                        "JOIN reporter.moderatorRelations mr " +
                        "WHERE c.offenseCount >= 1 " +
                        "AND mr.moderator.userId = :moderatorId " +
                        "AND mr.status = :status " +
                        "AND (CAST(:dateFrom AS timestamp) IS NULL OR c.createdAt >= :dateFrom) " +
                        "AND (CAST(:dateTo AS timestamp) IS NULL OR c.createdAt <= :dateTo)")
        Page<Comment> findReportedCommentsByModeratorId(
                        @Param("moderatorId") Integer moderatorId,
                        @Param("status") ModeratorReporter.Status status,
                        @Param("dateFrom") LocalDateTime dateFrom,
                        @Param("dateTo") LocalDateTime dateTo,
                        Pageable pageable);
}