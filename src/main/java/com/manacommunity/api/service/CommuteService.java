package com.manacommunity.api.service;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.domain.Page;

public interface CommuteService {

    Page<CommuteRideResponse> getUpcomingRides(AppUser user, String rideType, int page, int size);

    Page<CommuteRideResponse> searchRides(AppUser user, String destination, int page, int size);

    CommuteRideResponse getRide(AppUser user, Long rideId);

    CommuteRideResponse createRide(AppUser user, CreateCommuteRideRequest request);

    CommuteRideResponse updateRide(AppUser user, Long rideId, CreateCommuteRideRequest request);

    void cancelRide(AppUser user, Long rideId);

    Page<CommuteRideResponse> getMyOfferedRides(AppUser user, int page, int size);

    Page<CommuteRideResponse> getMyBookedRides(AppUser user, int page, int size);

    CommuteBookingResponse bookRide(AppUser user, Long rideId, CreateCommuteBookingRequest request);

    void cancelBooking(AppUser user, Long rideId);

    CommuteBookingResponse confirmBooking(AppUser user, Long bookingId);

    CommuteBookingResponse rejectBooking(AppUser user, Long bookingId);

    CommuteStatsResponse getStats(AppUser user);
}
