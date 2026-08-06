package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodMenuItemAddon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodMenuItemAddonRepository extends JpaRepository<FoodMenuItemAddon, Long> {

    List<FoodMenuItemAddon> findByItemId(Long itemId);
}
