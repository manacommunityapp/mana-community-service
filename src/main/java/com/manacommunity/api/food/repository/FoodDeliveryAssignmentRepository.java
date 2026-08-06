package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodDeliveryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodDeliveryAssignmentRepository extends JpaRepository<FoodDeliveryAssignment, Long> {

    Optional<FoodDeliveryAssignment> findByOrderId(Long orderId);

    List<FoodDeliveryAssignment> findByPartnerIdAndStatus(Long partnerId, String status);
}
