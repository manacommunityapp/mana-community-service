package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodNotificationPreferenceRepository extends JpaRepository<FoodNotificationPreference, Long> {

    Optional<FoodNotificationPreference> findByUserIdAndCommunityId(Long userId, Long communityId);
}
