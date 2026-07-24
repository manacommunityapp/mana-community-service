package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCommunityKitchenToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodCommunityKitchenTokenRepository extends JpaRepository<FoodCommunityKitchenToken, Long> {

    Optional<FoodCommunityKitchenToken> findByBookingId(Long bookingId);

    Optional<FoodCommunityKitchenToken> findByQrCode(String qrCode);
}
