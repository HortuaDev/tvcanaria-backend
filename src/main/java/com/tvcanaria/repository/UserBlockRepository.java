package com.tvcanaria.repository;

import com.tvcanaria.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Integer> {
    int countByUser_UserId(Integer userId);
}