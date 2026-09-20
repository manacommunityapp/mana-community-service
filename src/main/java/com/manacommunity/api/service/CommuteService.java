package com.manacommunity.api.service;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.domain.Page;

import java.util.List;

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

    // Ratings
    CommuteRatingResponse rateRide(AppUser user, Long rideId, CreateCommuteRatingRequest request);

    List<CommuteRatingResponse> getRideRatings(Long rideId);

    CommuteUserProfileResponse getUserProfile(AppUser currentUser, Long userId);

    // Vehicles
    List<CommuteVehicleResponse> getMyVehicles(AppUser user);

    CommuteVehicleResponse addVehicle(AppUser user, CreateCommuteVehicleRequest request);

    CommuteVehicleResponse updateVehicle(AppUser user, Long vehicleId, CreateCommuteVehicleRequest request);

    void deleteVehicle(AppUser user, Long vehicleId);

    // Favourite routes
    List<CommuteFavouriteRouteResponse> getMyFavouriteRoutes(AppUser user);

    CommuteFavouriteRouteResponse addFavouriteRoute(AppUser user, CreateCommuteFavouriteRouteRequest request);

    void deleteFavouriteRoute(AppUser user, Long routeId);
}
