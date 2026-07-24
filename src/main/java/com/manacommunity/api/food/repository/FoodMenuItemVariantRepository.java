package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodMenuItemVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodMenuItemVariantRepository extends JpaRepository<FoodMenuItemVariant, Long> {

    List<FoodMenuItemVariant> findByItemId(Long itemId);
}
