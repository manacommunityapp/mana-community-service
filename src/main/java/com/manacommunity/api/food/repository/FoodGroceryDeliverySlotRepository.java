package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodGroceryDeliverySlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FoodGroceryDeliverySlotRepository extends JpaRepository<FoodGroceryDeliverySlot, Long> {

    List<FoodGroceryDeliverySlot> findByStoreIdAndDate(Long storeId, LocalDate date);
}
