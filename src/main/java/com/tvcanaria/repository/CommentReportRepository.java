package com.tvcanaria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tvcanaria.entity.CommentReport;

public interface CommentReportRepository extends JpaRepository<CommentReport, Integer> {

    boolean existsByComment_CommentIdAndReporter_UserId(Integer commentId, Integer userId);

    long countByComment_CommentId(Integer commentId);

    List<CommentReportRepository> findByReviewedFalse();

    void deleteByComment_CommentId(Integer commentId);
}