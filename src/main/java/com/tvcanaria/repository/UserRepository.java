package com.tvcanaria.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tvcanaria.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUserId(Integer userId);

    Boolean existsByEmail(String email);

    Boolean existsByUsername(String username);

    Optional<User> findByProviderId(String providerId);

    // @Query("SELECT u FROM User u JOIN u.assignedReporters r WHERE r.userId =
    // :reporterId")
    // List<User> findModeratorsByReporter(Integer reporterId);

    // @Query("SELECT u FROM User u JOIN u.moderators m WHERE m.userId =
    // :moderatorId")
    // List<User> findReportersByModerator(Integer moderatorId);
}