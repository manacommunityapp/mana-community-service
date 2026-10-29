package com.manacommunity.api.trip.repository;

import com.manacommunity.api.trip.entity.TripBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripBookingRepository extends JpaRepository<TripBooking, String> {
    List<TripBooking> findByUserIdOrderByBookedAtDesc(Long userId);
    List<TripBooking> findByTripIdOrderByBookedAtDesc(String tripId);
    List<TripBooking> findByTripIdAndStatus(String tripId, String status);
}