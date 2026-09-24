package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommuteRide;
import com.manacommunity.api.model.CommuteRideStatus;
import com.manacommunity.api.model.CommuteRideType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommuteRideRepository extends JpaRepository<CommuteRide, Long> {

    @Query("SELECT r FROM CommuteRide r JOIN FETCH r.driver LEFT JOIN FETCH r.bookings " +
           "WHERE r.community.id = :communityId " +
           "AND r.status IN :statuses " +
           "AND r.departureTime >= :fromTime " +
           "AND (:rideType IS NULL OR r.rideType = :rideType) " +
           "ORDER BY r.departureTime ASC")
    Page<CommuteRide> findUpcomingRides(
            @Param("communityId") Long communityId,
            @Param("statuses") List<CommuteRideStatus> statuses,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("rideType") CommuteRideType rideType,
            Pageable pageable);

    @Query("SELECT r FROM CommuteRide r JOIN FETCH r.driver " +
           "WHERE r.community.id = :communityId " +
           "AND r.status = 'ACTIVE' " +
           "AND r.rideType = 'OFFER' " +
           "AND r.availableSeats > 0 " +
           "AND r.departureTime >= :fromTime " +
           "AND LOWER(r.toLocation) LIKE LOWER(CONCAT('%', :destination, '%')) " +
           "ORDER BY r.departureTime ASC")
    Page<CommuteRide> searchByDestination(
            @Param("communityId") Long communityId,
            @Param("destination") String destination,
            @Param("fromTime") LocalDateTime fromTime,
            Pageable pageable);

    @Query("SELECT r FROM CommuteRide r JOIN FETCH r.driver " +
           "WHERE r.driver.id = :userId " +
           "ORDER BY r.departureTime DESC")
    Page<CommuteRide> findByDriver(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT r FROM CommuteRide r JOIN FETCH r.driver JOIN r.bookings b " +
           "WHERE b.passenger.id = :userId " +
           "ORDER BY r.departureTime DESC")
    Page<CommuteRide> findByPassenger(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM CommuteRide r WHERE r.community.id = :communityId " +
           "AND r.status = 'ACTIVE' AND r.departureTime >= :fromTime")
    long countActiveRides(@Param("communityId") Long communityId, @Param("fromTime") LocalDateTime fromTime);

    @Query("SELECT COUNT(r) FROM CommuteRide r WHERE r.driver.id = :userId")
    long countByDriver(@Param("userId") Long userId);

    @Query("SELECT COUNT(b.ride) FROM CommuteBooking b WHERE b.passenger.id = :userId")
    long countByPassenger(@Param("userId") Long userId);

    @Query("SELECT r FROM CommuteRide r WHERE r.status IN ('ACTIVE', 'FULL') AND r.departureTime < :cutoff")
    Page<CommuteRide> findExpiredRides(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);
}
