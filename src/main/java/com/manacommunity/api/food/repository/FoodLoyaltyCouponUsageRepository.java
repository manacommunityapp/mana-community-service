package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodLoyaltyCouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodLoyaltyCouponUsageRepository extends JpaRepository<FoodLoyaltyCouponUsage, Long> {

    List<FoodLoyaltyCouponUsage> findByUserId(Long userId);

    List<FoodLoyaltyCouponUsage> findByCouponId(Long couponId);
}
