package com.manacommunity.api.trip.service;

import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.entity.TripBooking;

import java.util.List;
import java.util.Map;

public interface TripService {
    List<Trip> getTrips(String category);
    Trip getTrip(String id);
    TripBooking bookTrip(String tripId, Long userId, Map<String, Object> bookingPayload);
    List<TripBooking> getMyBookings(Long userId);
    Map<String, Object> cancelBooking(String bookingId, String reason);
    Trip createTrip(Trip trip);
    List<TripBooking> getManifest(String tripId);
    TripBooking checkInPassenger(String bookingId);
}