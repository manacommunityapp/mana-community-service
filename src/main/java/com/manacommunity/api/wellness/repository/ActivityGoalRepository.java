package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.ActivityGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActivityGoalRepository extends JpaRepository<ActivityGoal, Long> {
    Optional<ActivityGoal> findByUserId(Long userId);
}
