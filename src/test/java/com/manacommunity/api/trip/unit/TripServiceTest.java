package com.manacommunity.api.trip.unit;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.sports.controller.*;

import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.entity.TripBooking;
import com.manacommunity.api.trip.repository.TripBookingRepository;
import com.manacommunity.api.trip.repository.TripRepository;
import com.manacommunity.api.trip.service.TripServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Trip & Tour Service Unit Tests")
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripBookingRepository bookingRepository;

    @InjectMocks
    private TripServiceImpl tripService;

    private Trip testTrip;

    @BeforeEach
    void setUp() {
        testTrip = Trip.builder()
                .id("TRP-101")
                .communityId(1L)
                .title("Himalayan Valley Trek")
                .category("Trekking")
                .destination("Manali")
                .departureDate("2026-11-15")
                .departurePoint("Main Gate")
                .duration("4 Days / 3 Nights")
                .totalSeats(10)
                .bookedSeats(2)
                .pricePerPerson(new BigDecimal("5000.00"))
                .host("Rahul Verma")
                .hostFlat("A-301")
                .status("UPCOMING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully get trips by category")
    void shouldGetTripsByCategory() {
        when(tripRepository.findByCategoryIgnoreCaseOrderByDepartureDateAsc("Trekking"))
                .thenReturn(List.of(testTrip));

        List<Trip> result = tripService.getTrips("Trekking");
        assertEquals(1, result.size());
        assertEquals("Himalayan Valley Trek", result.get(0).getTitle());
    }

    @Test
    @DisplayName("Should book trip seats successfully and generate QR code")
    void shouldBookTripSeatsSuccessfully() {
        when(tripRepository.findById("TRP-101")).thenReturn(Optional.of(testTrip));
        when(bookingRepository.save(any(TripBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> payload = new HashMap<>();
        payload.put("passengers", List.of(Map.of("name", "Alice"), Map.of("name", "Bob")));
        payload.put("selectedPickupPoint", "Main Gate");

        TripBooking booking = tripService.bookTrip("TRP-101", 100L, payload);

        assertNotNull(booking);
        assertEquals(2, booking.getParticipantCount());
        assertEquals(new BigDecimal("10000.00"), booking.getTotalAmount());
        assertTrue(booking.getBoardingPassQR().contains("TRP-101"));
        assertEquals("CONFIRMED", booking.getStatus());
        assertEquals(4, testTrip.getBookedSeats());
    }

    @Test
    @DisplayName("Should throw exception when booking exceeds available seats")
    void shouldThrowExceptionWhenTripIsFull() {
        testTrip.setBookedSeats(9);
        when(tripRepository.findById("TRP-101")).thenReturn(Optional.of(testTrip));

        Map<String, Object> payload = new HashMap<>();
        payload.put("passengers", List.of(Map.of("name", "Alice"), Map.of("name", "Bob")));

        assertThrows(IllegalStateException.class, () -> tripService.bookTrip("TRP-101", 100L, payload));
    }

    @Test
    @DisplayName("Should cancel booking and restore seats to trip")
    void shouldCancelBookingAndRestoreSeats() {
        TripBooking booking = TripBooking.builder()
                .id("BKG-501")
                .tripId("TRP-101")
                .userId(100L)
                .participantCount(2)
                .totalAmount(new BigDecimal("10000.00"))
                .status("CONFIRMED")
                .build();

        testTrip.setBookedSeats(5);
        when(bookingRepository.findById("BKG-501")).thenReturn(Optional.of(booking));
        when(tripRepository.findById("TRP-101")).thenReturn(Optional.of(testTrip));

        Map<String, Object> result = tripService.cancelBooking("BKG-501", "Personal emergency");

        assertEquals(10000.0, result.get("refundAmount"));
        assertEquals("CANCELLED", booking.getStatus());
        assertEquals(3, testTrip.getBookedSeats());
    }

    @Test
    @DisplayName("Should check in passenger successfully")
    void shouldCheckInPassenger() {
        TripBooking booking = TripBooking.builder()
                .id("BKG-501")
                .tripId("TRP-101")
                .checkedIn(false)
                .build();

        when(bookingRepository.findById("BKG-501")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(TripBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        TripBooking updated = tripService.checkInPassenger("BKG-501");
        assertTrue(updated.getCheckedIn());
        assertNotNull(updated.getCheckedInAt());
    }
}
