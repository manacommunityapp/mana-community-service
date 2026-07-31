package com.cpn.domain.gamification.repository;

import com.cpn.domain.gamification.model.GamificationBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BadgeRepository extends JpaRepository<GamificationBadge, UUID> {
    List<GamificationBadge> findByUserId(UUID userId);
    List<GamificationBadge> findByUserIdAndIsUnlocked(UUID userId, boolean isUnlocked);
}
