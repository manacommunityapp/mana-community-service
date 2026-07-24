package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCommunityKitchenBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodCommunityKitchenBookingRepository extends JpaRepository<FoodCommunityKitchenBooking, Long> {

    List<FoodCommunityKitchenBooking> findByMenuId(Long menuId);

    Page<FoodCommunityKitchenBooking> findByUserIdAndCommunityId(Long userId, Long communityId, Pageable pageable);

    Optional<FoodCommunityKitchenBooking> findByIdAndCommunityId(Long id, Long communityId);
}
