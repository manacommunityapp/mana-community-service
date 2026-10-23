package com.manacommunity.api.trip.split.repository;

import com.manacommunity.api.trip.split.entity.TripSettlementPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripSettlementPaymentRepository extends JpaRepository<TripSettlementPayment, Long> {

    List<TripSettlementPayment> findByTripIdOrderByCreatedAtDesc(String tripId);

    List<TripSettlementPayment> findByTripIdAndStatus(String tripId, String status);
}
