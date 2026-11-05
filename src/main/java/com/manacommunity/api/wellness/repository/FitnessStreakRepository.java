package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.FitnessStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FitnessStreakRepository extends JpaRepository<FitnessStreak, Long> {
    Optional<FitnessStreak> findByUserId(Long userId);
}
