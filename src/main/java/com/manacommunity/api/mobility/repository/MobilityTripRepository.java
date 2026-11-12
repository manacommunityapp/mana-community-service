package com.manacommunity.api.mobility.repository;

import com.manacommunity.api.mobility.model.MobilityTrip;
import com.manacommunity.api.mobility.model.MobilityTripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MobilityTripRepository extends JpaRepository<MobilityTrip, Long> {

    @Query("SELECT t FROM MobilityTrip t WHERE t.passenger.id = :passengerId AND t.status IN :statuses")
    Optional<MobilityTrip> findActiveByPassenger(Long passengerId, List<MobilityTripStatus> statuses);

    Page<MobilityTrip> findByPassengerIdOrderByRequestedAtDesc(Long passengerId, Pageable pageable);

    @Query("SELECT t FROM MobilityTrip t WHERE t.driver.id = :driverId AND t.status IN :statuses")
    Optional<MobilityTrip> findActiveByDriver(Long driverId, List<MobilityTripStatus> statuses);

    Page<MobilityTrip> findByCommunityIdOrderByRequestedAtDesc(Long communityId, Pageable pageable);

    Page<MobilityTrip> findByStatusInOrderByRequestedAtDesc(List<MobilityTripStatus> statuses, Pageable pageable);

    boolean existsByPassengerIdAndIdempotencyKey(Long passengerId, String idempotencyKey);

    Optional<MobilityTrip> findByTripRef(String tripRef);
}
