package com.manacommunity.api.trip.p2p.repository;

import com.manacommunity.api.trip.p2p.entity.TripP2PCollectRequest;
import com.manacommunity.api.trip.p2p.entity.TripP2PCollectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripP2PCollectRequestRepository extends JpaRepository<TripP2PCollectRequest, Long> {

    List<TripP2PCollectRequest> findByTripId(String tripId);

    List<TripP2PCollectRequest> findByDebtorUserIdAndStatus(Long debtorUserId, TripP2PCollectStatus status);

    Optional<TripP2PCollectRequest> findByRazorpayPaymentId(String razorpayPaymentId);

    List<TripP2PCollectRequest> findByCreditorUserIdAndTripId(Long creditorUserId, String tripId);
}
