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

}