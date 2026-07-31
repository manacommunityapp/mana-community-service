package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodDeliveryZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodDeliveryZoneRepository extends JpaRepository<FoodDeliveryZone, Long> {

    List<FoodDeliveryZone> findByCommunityIdAndActive(Long communityId, Boolean active);
}
