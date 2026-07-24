package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodSubscriptionDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FoodSubscriptionDeliveryRepository extends JpaRepository<FoodSubscriptionDelivery, Long> {

    List<FoodSubscriptionDelivery> findBySubscriptionIdAndDate(Long subscriptionId, LocalDate date);

    Page<FoodSubscriptionDelivery> findBySubscriptionId(Long subscriptionId, Pageable pageable);
}
