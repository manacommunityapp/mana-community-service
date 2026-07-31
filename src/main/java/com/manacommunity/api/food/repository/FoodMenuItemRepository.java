package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodMenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FoodMenuItemRepository extends JpaRepository<FoodMenuItem, Long> {

    List<FoodMenuItem> findByCategoryId(Long categoryId);

    Page<FoodMenuItem> findByRestaurantIdAndIsAvailable(Long restaurantId, Boolean isAvailable, Pageable pageable);

    Page<FoodMenuItem> findByRestaurantId(Long restaurantId, Pageable pageable);

    @Query("SELECT i FROM FoodMenuItem i WHERE i.restaurant.id = :restaurantId " +
           "AND LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<FoodMenuItem> searchByRestaurant(@Param("restaurantId") Long restaurantId, @Param("query") String query, Pageable pageable);

    List<FoodMenuItem> findByRestaurantIdAndIsFeatured(Long restaurantId, Boolean isFeatured);

    List<FoodMenuItem> findByRestaurantIdAndIsBestseller(Long restaurantId, Boolean isBestseller);
}
