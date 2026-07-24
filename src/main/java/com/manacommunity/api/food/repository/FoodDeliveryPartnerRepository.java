package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodDeliveryPartner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodDeliveryPartnerRepository extends JpaRepository<FoodDeliveryPartner, Long> {

    Optional<FoodDeliveryPartner> findByUserIdAndCommunityId(Long userId, Long communityId);

    List<FoodDeliveryPartner> findByCommunityIdAndStatus(Long communityId, String status);

    Optional<FoodDeliveryPartner> findByIdAndCommunityId(Long id, Long communityId);
}
