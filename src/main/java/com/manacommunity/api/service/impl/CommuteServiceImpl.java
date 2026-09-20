package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.service.CommuteService;
import com.manacommunity.api.service.NotificationManagementService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommuteServiceImpl implements CommuteService {

    private final CommuteRideRepository rideRepository;
    private final CommuteBookingRepository bookingRepository;
    private final CommuteRatingRepository ratingRepository;
    private final CommuteVehicleRepository vehicleRepository;
    private final CommuteFavouriteRouteRepository favouriteRouteRepository;
    private final AppUserRepository appUserRepository;
    private final NotificationManagementService notificationService;

    @Override
    public Page<CommuteRideResponse> getUpcomingRides(AppUser user, String rideType, int page, int size) {
        CommuteRideType type = rideType != null ? CommuteRideType.valueOf(rideType) : null;
        List<CommuteRideStatus> statuses = List.of(CommuteRideStatus.ACTIVE);
        Page<CommuteRide> rides = rideRepository.findUpcomingRides(
                user.getCommunity().getId(), statuses, LocalDateTime.now(), type,
                PageRequest.of(page, size));
        return rides.map(r -> toResponse(r, user));
    }

    @Override
    public Page<CommuteRideResponse> searchRides(AppUser user, String destination, int page, int size) {
        Page<CommuteRide> rides = rideRepository.searchByDestination(
                user.getCommunity().getId(), destination, LocalDateTime.now(),
                PageRequest.of(page, size));
        return rides.map(r -> toResponse(r, user));
    }

    @Override
    public CommuteRideResponse getRide(AppUser user, Long rideId) {
        CommuteRide ride = findRideOrThrow(rideId);
        CommuteRideResponse response = toResponse(ride, user);
        if (ride.getDriver().getId().equals(user.getId())) {
            response.setBookings(ride.getBookings().stream().map(this::toBookingResponse).toList());
        }
        return response;
    }

    @Override
    @Transactional
    public CommuteRideResponse createRide(AppUser user, CreateCommuteRideRequest request) {
        CommuteRide ride = CommuteRide.builder()
                .driver(user)
                .community(user.getCommunity())
                .fromLocation(request.getFromLocation())
                .toLocation(request.getToLocation())
                .fromLat(request.getFromLat())
                .fromLng(request.getFromLng())
                .toLat(request.getToLat())
                .toLng(request.getToLng())
                .departureTime(request.getDepartureTime())
                .rideType(CommuteRideType.valueOf(request.getRideType()))
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .pricePerSeat(request.getPricePerSeat())
                .free(request.isFree())
                .vehicleType(request.getVehicleType())
                .vehicleNumber(request.getVehicleNumber())
                .notes(request.getNotes())
                .recurring(request.isRecurring())
                .recurringDays(request.getRecurringDays())
                .recurringTime(request.getRecurringTime())
                .ladiesOnly(request.isLadiesOnly())
                .build();
        return toResponse(rideRepository.save(ride), user);
    }

    @Override
    @Transactional
    public CommuteRideResponse updateRide(AppUser user, Long rideId, CreateCommuteRideRequest request) {
        CommuteRide ride = findRideOrThrow(rideId);
        validateOwner(ride, user);

        ride.setFromLocation(request.getFromLocation());
        ride.setToLocation(request.getToLocation());
        ride.setFromLat(request.getFromLat());
        ride.setFromLng(request.getFromLng());
        ride.setToLat(request.getToLat());
        ride.setToLng(request.getToLng());
        ride.setDepartureTime(request.getDepartureTime());
        ride.setTotalSeats(request.getTotalSeats());
        ride.setPricePerSeat(request.getPricePerSeat());
        ride.setFree(request.isFree());
        ride.setVehicleType(request.getVehicleType());
        ride.setVehicleNumber(request.getVehicleNumber());
        ride.setNotes(request.getNotes());
        ride.setRecurring(request.isRecurring());
        ride.setRecurringDays(request.getRecurringDays());
        ride.setRecurringTime(request.getRecurringTime());
        ride.setLadiesOnly(request.isLadiesOnly());

        return toResponse(rideRepository.save(ride), user);
    }

    @Override
    @Transactional
    public void cancelRide(AppUser user, Long rideId) {
        CommuteRide ride = findRideOrThrow(rideId);
        validateOwner(ride, user);
        ride.setStatus(CommuteRideStatus.CANCELLED);
        rideRepository.save(ride);

        ride.getBookings().stream()
                .filter(b -> b.getStatus() == CommuteBookingStatus.PENDING
                        || b.getStatus() == CommuteBookingStatus.CONFIRMED)
                .forEach(b -> {
                    b.setStatus(CommuteBookingStatus.CANCELLED);
                    bookingRepository.save(b);
                    notificationService.createNotification(
                            b.getPassenger().getId(),
                            NotificationType.COMMUTE_RIDE_CANCELLED,
                            NotificationCategory.COMMUTE,
                            "Ride Cancelled",
                            user.getFullName() + " cancelled the ride from " + ride.getFromLocation() + " to " + ride.getToLocation(),
                            "/commute/ride/" + ride.getId(),
                            ReferenceType.COMMUTE_RIDE,
                            ride.getId(),
                            NotificationPriority.HIGH,
                            null,
                            ride.getCommunity().getId()
                    );
                });
    }

    @Override
    public Page<CommuteRideResponse> getMyOfferedRides(AppUser user, int page, int size) {
        return rideRepository.findByDriver(user.getId(), PageRequest.of(page, size))
                .map(r -> toResponse(r, user));
    }

    @Override
    public Page<CommuteRideResponse> getMyBookedRides(AppUser user, int page, int size) {
        return rideRepository.findByPassenger(user.getId(), PageRequest.of(page, size))
                .map(r -> toResponse(r, user));
    }

    @Override
    @Transactional
    public CommuteBookingResponse bookRide(AppUser user, Long rideId, CreateCommuteBookingRequest request) {
        CommuteRide ride = findRideOrThrow(rideId);

        if (ride.getDriver().getId().equals(user.getId())) {
            throw new IllegalStateException("Cannot book your own ride");
        }
        if (ride.getStatus() != CommuteRideStatus.ACTIVE) {
            throw new IllegalStateException("Ride is not available for booking");
        }
        if (bookingRepository.existsByRideIdAndPassengerId(rideId, user.getId())) {
            throw new IllegalStateException("You have already booked this ride");
        }

        int seats = Math.max(request.getSeatsBooked(), 1);
        if (seats > ride.getAvailableSeats()) {
            throw new IllegalStateException("Not enough seats available");
        }

        CommuteBooking booking = CommuteBooking.builder()
                .ride(ride)
                .passenger(user)
                .seatsBooked(seats)
                .pickupNote(request.getPickupNote())
                .build();

        ride.setAvailableSeats(ride.getAvailableSeats() - seats);
        if (ride.getAvailableSeats() == 0) {
            ride.setStatus(CommuteRideStatus.FULL);
        }
        rideRepository.save(ride);

        CommuteBooking saved = bookingRepository.save(booking);

        notificationService.createNotification(
                ride.getDriver().getId(),
                NotificationType.COMMUTE_BOOKING_RECEIVED,
                NotificationCategory.COMMUTE,
                "New Booking Request",
                user.getFullName() + " requested " + seats + " seat(s) on your ride to " + ride.getToLocation(),
                "/commute/ride/" + ride.getId(),
                ReferenceType.COMMUTE_BOOKING,
                saved.getId(),
                NotificationPriority.NORMAL,
                null,
                ride.getCommunity().getId()
        );

        return toBookingResponse(saved);
    }

    @Override
    @Transactional
    public void cancelBooking(AppUser user, Long rideId) {
        CommuteBooking booking = bookingRepository.findByRideIdAndPassengerId(rideId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        booking.setStatus(CommuteBookingStatus.CANCELLED);
        bookingRepository.save(booking);

        CommuteRide ride = booking.getRide();
        ride.setAvailableSeats(ride.getAvailableSeats() + booking.getSeatsBooked());
        if (ride.getStatus() == CommuteRideStatus.FULL) {
            ride.setStatus(CommuteRideStatus.ACTIVE);
        }
        rideRepository.save(ride);

        notificationService.createNotification(
                ride.getDriver().getId(),
                NotificationType.COMMUTE_BOOKING_CANCELLED,
                NotificationCategory.COMMUTE,
                "Booking Cancelled",
                user.getFullName() + " cancelled their booking on your ride to " + ride.getToLocation(),
                "/commute/ride/" + ride.getId(),
                ReferenceType.COMMUTE_BOOKING,
                booking.getId(),
                NotificationPriority.NORMAL,
                null,
                ride.getCommunity().getId()
        );
    }

    @Override
    @Transactional
    public CommuteBookingResponse confirmBooking(AppUser user, Long bookingId) {
        CommuteBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        validateOwner(booking.getRide(), user);
        booking.setStatus(CommuteBookingStatus.CONFIRMED);
        CommuteBooking saved = bookingRepository.save(booking);

        notificationService.createNotification(
                booking.getPassenger().getId(),
                NotificationType.COMMUTE_BOOKING_CONFIRMED,
                NotificationCategory.COMMUTE,
                "Booking Confirmed",
                user.getFullName() + " confirmed your ride to " + booking.getRide().getToLocation(),
                "/commute/ride/" + booking.getRide().getId(),
                ReferenceType.COMMUTE_BOOKING,
                booking.getId(),
                NotificationPriority.NORMAL,
                null,
                booking.getRide().getCommunity().getId()
        );

        return toBookingResponse(saved);
    }

    @Override
    @Transactional
    public CommuteBookingResponse rejectBooking(AppUser user, Long bookingId) {
        CommuteBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        validateOwner(booking.getRide(), user);

        booking.setStatus(CommuteBookingStatus.REJECTED);
        bookingRepository.save(booking);

        CommuteRide ride = booking.getRide();
        ride.setAvailableSeats(ride.getAvailableSeats() + booking.getSeatsBooked());
        if (ride.getStatus() == CommuteRideStatus.FULL) {
            ride.setStatus(CommuteRideStatus.ACTIVE);
        }
        rideRepository.save(ride);

        notificationService.createNotification(
                booking.getPassenger().getId(),
                NotificationType.COMMUTE_BOOKING_REJECTED,
                NotificationCategory.COMMUTE,
                "Booking Declined",
                user.getFullName() + " declined your booking request for the ride to " + ride.getToLocation(),
                "/commute/ride/" + ride.getId(),
                ReferenceType.COMMUTE_BOOKING,
                booking.getId(),
                NotificationPriority.NORMAL,
                null,
                ride.getCommunity().getId()
        );

        return toBookingResponse(booking);
    }

    @Override
    public CommuteStatsResponse getStats(AppUser user) {
        long active = rideRepository.countActiveRides(user.getCommunity().getId(), LocalDateTime.now());
        long offered = rideRepository.findByDriver(user.getId(), PageRequest.of(0, 1)).getTotalElements();
        long booked = rideRepository.findByPassenger(user.getId(), PageRequest.of(0, 1)).getTotalElements();
        return CommuteStatsResponse.builder()
                .activeRides(active)
                .myOfferedRides(offered)
                .myBookedRides(booked)
                .build();
    }

    // ── Ratings ──

    @Override
    @Transactional
    public CommuteRatingResponse rateRide(AppUser user, Long rideId, CreateCommuteRatingRequest request) {
        CommuteRide ride = findRideOrThrow(rideId);

        if (ride.getStatus() != CommuteRideStatus.COMPLETED) {
            throw new IllegalStateException("Can only rate completed rides");
        }
        if (ratingRepository.findByRideIdAndRaterId(rideId, user.getId()).isPresent()) {
            throw new IllegalStateException("You have already rated this ride");
        }

        boolean isDriver = ride.getDriver().getId().equals(user.getId());
        boolean isPassenger = ride.getBookings().stream()
                .anyMatch(b -> b.getPassenger().getId().equals(user.getId())
                        && b.getStatus() == CommuteBookingStatus.CONFIRMED);

        if (!isDriver && !isPassenger) {
            throw new IllegalStateException("Only participants can rate a ride");
        }

        AppUser rated;
        if (isDriver) {
            CommuteBooking firstConfirmed = ride.getBookings().stream()
                    .filter(b -> b.getStatus() == CommuteBookingStatus.CONFIRMED)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No confirmed passengers to rate"));
            rated = firstConfirmed.getPassenger();
        } else {
            rated = ride.getDriver();
        }

        CommuteRating rating = CommuteRating.builder()
                .ride(ride)
                .rater(user)
                .rated(rated)
                .score(request.getScore())
                .comment(request.getComment())
                .build();

        return toRatingResponse(ratingRepository.save(rating));
    }

    @Override
    public List<CommuteRatingResponse> getRideRatings(Long rideId) {
        return ratingRepository.findByRideId(rideId).stream()
                .map(this::toRatingResponse)
                .toList();
    }

    @Override
    public CommuteUserProfileResponse getUserProfile(AppUser currentUser, Long userId) {
        AppUser target = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        double avg = ratingRepository.getAverageRating(userId);
        long count = ratingRepository.getRatingCount(userId);
        long offered = rideRepository.findByDriver(userId, PageRequest.of(0, 1)).getTotalElements();
        long booked = rideRepository.findByPassenger(userId, PageRequest.of(0, 1)).getTotalElements();

        return CommuteUserProfileResponse.builder()
                .userId(target.getId())
                .name(target.getFullName())
                .flat(target.getFlatNo())
                .photo(target.getProfilePicUrl())
                .averageRating(avg)
                .totalRatings(count)
                .ridesOffered(offered)
                .ridesBooked(booked)
                .kycVerified("VERIFIED".equalsIgnoreCase(target.getKycStatus()))
                .build();
    }

    // ── Vehicles ──

    @Override
    public List<CommuteVehicleResponse> getMyVehicles(AppUser user) {
        return vehicleRepository.findByOwnerIdOrderByIsDefaultDesc(user.getId()).stream()
                .map(this::toVehicleResponse)
                .toList();
    }

    @Override
    @Transactional
    public CommuteVehicleResponse addVehicle(AppUser user, CreateCommuteVehicleRequest request) {
        if (request.isDefault()) {
            vehicleRepository.findByOwnerIdAndIsDefaultTrue(user.getId())
                    .ifPresent(v -> {
                        v.setDefault(false);
                        vehicleRepository.save(v);
                    });
        }

        CommuteVehicle vehicle = CommuteVehicle.builder()
                .owner(user)
                .vehicleType(request.getVehicleType())
                .model(request.getModel())
                .color(request.getColor())
                .numberPlate(request.getNumberPlate())
                .isDefault(request.isDefault())
                .build();

        return toVehicleResponse(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public CommuteVehicleResponse updateVehicle(AppUser user, Long vehicleId, CreateCommuteVehicleRequest request) {
        CommuteVehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        if (!vehicle.getOwner().getId().equals(user.getId())) {
            throw new IllegalStateException("Only the owner can update this vehicle");
        }

        if (request.isDefault() && !vehicle.isDefault()) {
            vehicleRepository.findByOwnerIdAndIsDefaultTrue(user.getId())
                    .ifPresent(v -> {
                        v.setDefault(false);
                        vehicleRepository.save(v);
                    });
        }

        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setModel(request.getModel());
        vehicle.setColor(request.getColor());
        vehicle.setNumberPlate(request.getNumberPlate());
        vehicle.setDefault(request.isDefault());

        return toVehicleResponse(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public void deleteVehicle(AppUser user, Long vehicleId) {
        CommuteVehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        if (!vehicle.getOwner().getId().equals(user.getId())) {
            throw new IllegalStateException("Only the owner can delete this vehicle");
        }
        vehicleRepository.delete(vehicle);
    }

    // ── Favourite Routes ──

    @Override
    public List<CommuteFavouriteRouteResponse> getMyFavouriteRoutes(AppUser user) {
        return favouriteRouteRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toFavRouteResponse)
                .toList();
    }

    @Override
    @Transactional
    public CommuteFavouriteRouteResponse addFavouriteRoute(AppUser user, CreateCommuteFavouriteRouteRequest request) {
        CommuteFavouriteRoute route = CommuteFavouriteRoute.builder()
                .user(user)
                .label(request.getLabel())
                .fromLocation(request.getFromLocation())
                .toLocation(request.getToLocation())
                .fromLat(request.getFromLat())
                .fromLng(request.getFromLng())
                .toLat(request.getToLat())
                .toLng(request.getToLng())
                .build();
        return toFavRouteResponse(favouriteRouteRepository.save(route));
    }

    @Override
    @Transactional
    public void deleteFavouriteRoute(AppUser user, Long routeId) {
        CommuteFavouriteRoute route = favouriteRouteRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Favourite route not found"));
        if (!route.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Only the owner can delete this route");
        }
        favouriteRouteRepository.delete(route);
    }

    // ── Private helpers ──

    private CommuteRide findRideOrThrow(Long rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));
    }

    private void validateOwner(CommuteRide ride, AppUser user) {
        if (!ride.getDriver().getId().equals(user.getId())) {
            throw new IllegalStateException("Only the ride owner can perform this action");
        }
    }

    private CommuteRideResponse toResponse(CommuteRide ride, AppUser currentUser) {
        Optional<CommuteBooking> myBooking = ride.getBookings().stream()
                .filter(b -> b.getPassenger().getId().equals(currentUser.getId()))
                .findFirst();

        double driverRating = ratingRepository.getAverageRating(ride.getDriver().getId());

        return CommuteRideResponse.builder()
                .id(ride.getId())
                .driverId(ride.getDriver().getId())
                .driverName(ride.getDriver().getFullName())
                .driverFlat(ride.getDriver().getFlatNo())
                .driverPhoto(ride.getDriver().getProfilePicUrl())
                .driverRating(driverRating)
                .fromLocation(ride.getFromLocation())
                .toLocation(ride.getToLocation())
                .fromLat(ride.getFromLat())
                .fromLng(ride.getFromLng())
                .toLat(ride.getToLat())
                .toLng(ride.getToLng())
                .departureTime(ride.getDepartureTime())
                .rideType(ride.getRideType().name())
                .totalSeats(ride.getTotalSeats())
                .availableSeats(ride.getAvailableSeats())
                .pricePerSeat(ride.getPricePerSeat())
                .free(ride.isFree())
                .vehicleType(ride.getVehicleType())
                .vehicleNumber(ride.getVehicleNumber())
                .notes(ride.getNotes())
                .status(ride.getStatus().name())
                .recurring(ride.isRecurring())
                .recurringDays(ride.getRecurringDays())
                .recurringTime(ride.getRecurringTime())
                .ladiesOnly(ride.isLadiesOnly())
                .distanceKm(ride.getDistanceKm())
                .bookingCount((int) ride.getBookings().stream()
                        .filter(b -> b.getStatus() != CommuteBookingStatus.CANCELLED
                                && b.getStatus() != CommuteBookingStatus.REJECTED)
                        .count())
                .isMyRide(ride.getDriver().getId().equals(currentUser.getId()))
                .hasBooked(myBooking.isPresent())
                .myBookingStatus(myBooking.map(b -> b.getStatus().name()).orElse(null))
                .createdAt(ride.getCreatedAt())
                .build();
    }

    private CommuteBookingResponse toBookingResponse(CommuteBooking booking) {
        return CommuteBookingResponse.builder()
                .id(booking.getId())
                .passengerId(booking.getPassenger().getId())
                .passengerName(booking.getPassenger().getFullName())
                .passengerFlat(booking.getPassenger().getFlatNo())
                .passengerPhoto(booking.getPassenger().getProfilePicUrl())
                .seatsBooked(booking.getSeatsBooked())
                .pickupNote(booking.getPickupNote())
                .status(booking.getStatus().name())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private CommuteRatingResponse toRatingResponse(CommuteRating rating) {
        return CommuteRatingResponse.builder()
                .id(rating.getId())
                .rideId(rating.getRide().getId())
                .raterId(rating.getRater().getId())
                .raterName(rating.getRater().getFullName())
                .ratedId(rating.getRated().getId())
                .ratedName(rating.getRated().getFullName())
                .score(rating.getScore())
                .comment(rating.getComment())
                .createdAt(rating.getCreatedAt())
                .build();
    }

    private CommuteVehicleResponse toVehicleResponse(CommuteVehicle vehicle) {
        return CommuteVehicleResponse.builder()
                .id(vehicle.getId())
                .vehicleType(vehicle.getVehicleType())
                .model(vehicle.getModel())
                .color(vehicle.getColor())
                .numberPlate(vehicle.getNumberPlate())
                .isDefault(vehicle.isDefault())
                .createdAt(vehicle.getCreatedAt())
                .build();
    }

    private CommuteFavouriteRouteResponse toFavRouteResponse(CommuteFavouriteRoute route) {
        return CommuteFavouriteRouteResponse.builder()
                .id(route.getId())
                .label(route.getLabel())
                .fromLocation(route.getFromLocation())
                .toLocation(route.getToLocation())
                .fromLat(route.getFromLat())
                .fromLng(route.getFromLng())
                .toLat(route.getToLat())
                .toLng(route.getToLng())
                .createdAt(route.getCreatedAt())
                .build();
    }
}
