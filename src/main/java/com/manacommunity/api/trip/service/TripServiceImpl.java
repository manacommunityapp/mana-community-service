package com.manacommunity.api.trip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.entity.TripBooking;
import com.manacommunity.api.trip.repository.TripBookingRepository;
import com.manacommunity.api.trip.repository.TripRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final TripBookingRepository bookingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    @Transactional
    public void initDefaultTrips() {
        if (tripRepository.count() > 0) return;
        log.info("[TripService] Seeding default community trips");

        seedTrip("TRP-1", "Kedarnath Spiritual Yatra", "Pilgrimage", "Kedarnath, Uttarakhand",
                "Oct 25, 2026", "Main Gate Society Bus Bay", "5 Days / 4 Nights", 35, 24,
                new BigDecimal("18500.00"), "Suresh Iyer", "A-401", "AC Volvo Coach");

        seedTrip("TRP-2", "Coorg Coffee Trail & Waterfall Trek", "Trekking", "Coorg, Karnataka",
                "Nov 12, 2026", "Clubhouse Parking Bay", "2 Days / 1 Night", 20, 16,
                new BigDecimal("4200.00"), "Ananya Sharma", "B-204", "Tempo Traveller AC");

        seedTrip("TRP-3", "Pawna Lake Stargazing & Camping", "Camping", "Pawna Lake, Lonavala",
                "Nov 28, 2026", "Society Gate 2", "Overnight Camp", 25, 11,
                new BigDecimal("2800.00"), "Rahul Deshmukh", "C-102", "Community Carpool / Bus");

        seedTrip("TRP-4", "Gokarna Sunset & Beach Trek", "Beach & Coastal", "Gokarna, Karnataka",
                "Dec 18, 2026", "Main Gate Bay", "3 Days / 2 Nights", 30, 8,
                new BigDecimal("5900.00"), "Vikram Mehta", "D-802", "Luxury Mini Coach");
    }

    private void seedTrip(String id, String title, String category, String dest,
                          String date, String point, String dur, int total, int booked,
                          BigDecimal price, String host, String flat, String transport) {
        tripRepository.save(Trip.builder()
                .id(id)
                .title(title)
                .category(category)
                .destination(dest)
                .departureDate(date)
                .departurePoint(point)
                .duration(dur)
                .totalSeats(total)
                .bookedSeats(booked)
                .pricePerPerson(price)
                .host(host)
                .hostFlat(flat)
                .transport(transport)
                .status("UPCOMING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Override
    public List<Trip> getTrips(String category) {
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")) {
            return tripRepository.findByCategoryIgnoreCaseOrderByDepartureDateAsc(category);
        }
        return tripRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Trip getTrip(String id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + id));
    }

    @Override
    @Transactional
    public TripBooking bookTrip(String tripId, Long userId, Map<String, Object> payload) {
        Trip trip = getTrip(tripId);

        List<?> rawPassengers = (List<?>) payload.get("passengers");
        int paxCount = (rawPassengers != null && !rawPassengers.isEmpty()) ? rawPassengers.size() : 1;

        if (trip.getBookedSeats() + paxCount > trip.getTotalSeats()) {
            throw new IllegalStateException("Not enough seats available. Remaining seats: " + (trip.getTotalSeats() - trip.getBookedSeats()));
        }

        trip.setBookedSeats(trip.getBookedSeats() + paxCount);
        tripRepository.save(trip);

        String passengersJson = null;
        try {
            passengersJson = objectMapper.writeValueAsString(rawPassengers);
        } catch (Exception ignored) {}

        BigDecimal totalAmount = trip.getPricePerPerson().multiply(BigDecimal.valueOf(paxCount));
        String bookingId = "BKG-" + (System.currentTimeMillis() % 100000L);
        String qr = "MANA-" + trip.getId() + "-U" + userId + "-" + paxCount + "PAX-" + bookingId;

        TripBooking booking = TripBooking.builder()
                .id(bookingId)
                .tripId(trip.getId())
                .userId(userId)
                .tripTitle(trip.getTitle())
                .destination(trip.getDestination())
                .departureDate(trip.getDepartureDate())
                .participantCount(paxCount)
                .passengersJson(passengersJson)
                .selectedPickupPoint((String) payload.getOrDefault("selectedPickupPoint", trip.getDeparturePoint()))
                .selectedRoomType((String) payload.get("selectedRoomType"))
                .totalAmount(totalAmount)
                .status("CONFIRMED")
                .boardingPassQR(qr)
                .host(trip.getHost() + " (" + trip.getHostFlat() + ")")
                .bookedAt(LocalDateTime.now())
                .checkedIn(false)
                .build();

        return bookingRepository.save(booking);
    }

    @Override
    public List<TripBooking> getMyBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByBookedAtDesc(userId);
    }

    @Override
    @Transactional
    public Map<String, Object> cancelBooking(String bookingId, String reason) {
        TripBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

        Optional<Trip> tripOpt = tripRepository.findById(booking.getTripId());
        if (tripOpt.isPresent()) {
            Trip trip = tripOpt.get();
            int newSeats = Math.max(0, trip.getBookedSeats() - booking.getParticipantCount());
            trip.setBookedSeats(newSeats);
            tripRepository.save(trip);
        }

        return Map.of("refundAmount", booking.getTotalAmount().doubleValue(), "penaltyDeducted", 0.0);
    }

    @Override
    @Transactional
    public Trip createTrip(Trip trip) {
        if (trip.getId() == null || trip.getId().isBlank()) {
            trip.setId("TRP-" + (System.currentTimeMillis() % 10000L));
        }
        trip.setCreatedAt(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());
        return tripRepository.save(trip);
    }

    @Override
    public List<TripBooking> getManifest(String tripId) {
        return bookingRepository.findByTripIdOrderByBookedAtDesc(tripId);
    }

    @Override
    @Transactional
    public TripBooking checkInPassenger(String bookingId) {
        TripBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        booking.setCheckedIn(true);
        booking.setCheckedInAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }
}