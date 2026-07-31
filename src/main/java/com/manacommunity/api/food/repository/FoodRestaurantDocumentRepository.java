package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodRestaurantDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodRestaurantDocumentRepository extends JpaRepository<FoodRestaurantDocument, Long> {

    List<FoodRestaurantDocument> findByRestaurantId(Long restaurantId);
}
