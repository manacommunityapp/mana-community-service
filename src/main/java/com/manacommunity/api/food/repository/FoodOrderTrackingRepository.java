package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodOrderTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodOrderTrackingRepository extends JpaRepository<FoodOrderTracking, Long> {

    List<FoodOrderTracking> findByOrderIdOrderByTimestampDesc(Long orderId);
}
