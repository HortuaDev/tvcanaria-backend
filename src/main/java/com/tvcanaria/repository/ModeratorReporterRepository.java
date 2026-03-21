package com.tvcanaria.repository;

import com.tvcanaria.entity.ModeratorReporter;
import com.tvcanaria.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModeratorReporterRepository extends JpaRepository<ModeratorReporter, Integer> {

    List<ModeratorReporter> findByModerator_UserIdAndStatus(
            Integer moderatorId, ModeratorReporter.Status status);

    List<ModeratorReporter> findByReporter_UserId(Integer reporterId);

    Optional<ModeratorReporter> findByModerator_UserIdAndReporter_UserId(
            Integer moderatorId, Integer reporterId);

    @Query("SELECT mr.moderator FROM ModeratorReporter mr WHERE mr.reporter.userId = :reporterId AND mr.status = :status")
    List<User> findAcceptedModeratorsByReporter(
            @Param("reporterId") Integer reporterId,
            @Param("status") ModeratorReporter.Status status);

    @Query("SELECT mr.reporter FROM ModeratorReporter mr WHERE mr.moderator.userId = :moderatorId AND mr.status = :status")
    List<User> findAcceptedReportersByModerator(
            @Param("moderatorId") Integer moderatorId,
            @Param("status") ModeratorReporter.Status status);
}