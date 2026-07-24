package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodNotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodNotificationLogRepository extends JpaRepository<FoodNotificationLog, Long> {

    Page<FoodNotificationLog> findByUserIdAndCommunityId(Long userId, Long communityId, Pageable pageable);

    List<FoodNotificationLog> findByUserIdAndStatus(Long userId, String status);
}
