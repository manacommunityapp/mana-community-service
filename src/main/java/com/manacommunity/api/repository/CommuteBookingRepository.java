package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommuteBooking;
import com.manacommunity.api.model.CommuteBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommuteBookingRepository extends JpaRepository<CommuteBooking, Long> {

    Optional<CommuteBooking> findByRideIdAndPassengerId(Long rideId, Long passengerId);

    List<CommuteBooking> findByRideIdAndStatus(Long rideId, CommuteBookingStatus status);

    @Query("SELECT b FROM CommuteBooking b WHERE b.ride.id = :rideId ORDER BY b.createdAt ASC")
    List<CommuteBooking> findByRideId(@Param("rideId") Long rideId);

    boolean existsByRideIdAndPassengerId(Long rideId, Long passengerId);
}
