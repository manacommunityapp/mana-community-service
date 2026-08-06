package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodLoyaltyCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodLoyaltyCouponRepository extends JpaRepository<FoodLoyaltyCoupon, Long> {

    Optional<FoodLoyaltyCoupon> findByCodeAndActive(String code, Boolean active);

    List<FoodLoyaltyCoupon> findByCommunityIdAndActive(Long communityId, Boolean active);
}
