package com.manacommunity.api.mobility.repository;

import com.manacommunity.api.mobility.model.MobilityDriverOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface MobilityDriverOfferRepository extends JpaRepository<MobilityDriverOffer, Long> {

    @Query("SELECT o FROM MobilityDriverOffer o WHERE o.driver.id = :driverId AND o.accepted IS NULL AND o.expiresAt > :now")
    Optional<MobilityDriverOffer> findPendingByDriver(Long driverId, Instant now);

    @Query("SELECT o FROM MobilityDriverOffer o WHERE o.trip.id = :tripId AND o.accepted IS NULL AND o.expiresAt > :now")
    Optional<MobilityDriverOffer> findPendingByTrip(Long tripId, Instant now);

    void deleteByTripIdAndAcceptedIsNull(Long tripId);
}
