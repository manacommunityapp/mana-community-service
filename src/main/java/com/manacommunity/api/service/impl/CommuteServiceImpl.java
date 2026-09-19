package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.CommuteBookingRepository;
import com.manacommunity.api.repository.CommuteRideRepository;
import com.manacommunity.api.service.CommuteService;
import com.manacommunity.api.user.model.AppUser;
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

        return toBookingResponse(bookingRepository.save(booking));
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
    }

    @Override
    @Transactional
    public CommuteBookingResponse confirmBooking(AppUser user, Long bookingId) {
        CommuteBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        validateOwner(booking.getRide(), user);
        booking.setStatus(CommuteBookingStatus.CONFIRMED);
        return toBookingResponse(bookingRepository.save(booking));
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

        return CommuteRideResponse.builder()
                .id(ride.getId())
                .driverId(ride.getDriver().getId())
                .driverName(ride.getDriver().getName())
                .driverFlat(ride.getDriver().getFlatNumber())
                .driverPhoto(ride.getDriver().getProfilePhoto())
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
                .passengerName(booking.getPassenger().getName())
                .passengerFlat(booking.getPassenger().getFlatNumber())
                .passengerPhoto(booking.getPassenger().getProfilePhoto())
                .seatsBooked(booking.getSeatsBooked())
                .pickupNote(booking.getPickupNote())
                .status(booking.getStatus().name())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
