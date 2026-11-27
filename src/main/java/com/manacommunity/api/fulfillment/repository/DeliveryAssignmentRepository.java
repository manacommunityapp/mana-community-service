package com.manacommunity.api.fulfillment.repository;

import com.manacommunity.api.fulfillment.entity.DeliveryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {

    Optional<DeliveryAssignment> findTopByOrderIdOrderByCreatedAtDesc(String orderId);
}
