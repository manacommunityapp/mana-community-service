package com.manacommunity.api.repository;

import com.manacommunity.api.model.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUserIdAndCommunityIdOrderByEarnedAtDesc(Long userId, Long communityId);
    List<UserBadge> findByUserIdOrderByEarnedAtDesc(Long userId);
    boolean existsByUserIdAndBadgeType(Long userId, String badgeType);
}
