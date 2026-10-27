package com.manacommunity.api.mobility.repository;

import com.manacommunity.api.mobility.model.MobilityAirportRideRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface MobilityAirportRideRequestRepository extends JpaRepository<MobilityAirportRideRequest, Long> {
    Page<MobilityAirportRideRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId, Pageable pageable);
    java.util.Optional<MobilityAirportRideRequest> findByIdAndRequesterId(Long id, Long requesterId);
    List<MobilityAirportRideRequest> findByStatusAndTravelAtBetweenOrderByTravelAtAsc(String status, Instant from, Instant to);
    List<MobilityAirportRideRequest> findByStatusAndTravelAtBefore(String status, Instant before);
}
