package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodPantryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FoodPantryItemRepository extends JpaRepository<FoodPantryItem, Long> {

    List<FoodPantryItem> findByUserIdAndCommunityId(Long userId, Long communityId);

    List<FoodPantryItem> findByUserIdAndStatus(Long userId, String status);

    List<FoodPantryItem> findByUserIdAndExpiryDateBefore(Long userId, LocalDate date);
}
