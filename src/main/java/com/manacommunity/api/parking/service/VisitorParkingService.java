package com.manacommunity.api.parking.service;

import com.manacommunity.api.parking.entity.ParkingBay;
import com.manacommunity.api.parking.entity.ParkingBooking;
import com.manacommunity.api.parking.repository.ParkingBayRepository;
import com.manacommunity.api.parking.repository.ParkingBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitorParkingService {

    private final ParkingBayRepository bayRepository;
    private final ParkingBookingRepository bookingRepository;

    @Transactional
    public AllocationResult allocateVisitorBay(Long communityId, String vehicleNumber,
                                                String vehicleType, String visitorName,
                                                String hostFlat, String preferredBasement) {
        String clearance = mapVehicleClearance(vehicleType);

        List<ParkingBay> available;
        if (preferredBasement != null) {
            available = bayRepository.findAvailableVisitorBaysInBasement(communityId, preferredBasement, clearance);
            if (available.isEmpty()) {
                available = bayRepository.findAvailableVisitorBays(communityId, clearance);
            }
        } else {
            available = bayRepository.findAvailableVisitorBays(communityId, clearance);
        }

        if (available.isEmpty()) {
            return new AllocationResult(false, null, null, null,
                    "No visitor parking available for " + vehicleType, 0);
        }

        ParkingBay bay = available.get(0);
        ParkingBay locked = bayRepository.findByIdWithLock(bay.getId())
                .orElseThrow(() -> new IllegalStateException("Bay not found"));

        if (!"AVAILABLE".equals(locked.getStatus())) {
            available.remove(0);
            if (available.isEmpty()) {
                return new AllocationResult(false, null, null, null,
                        "Bay was taken by concurrent request, no alternatives", 0);
            }
            bay = available.get(0);
            locked = bayRepository.findByIdWithLock(bay.getId())
                    .orElseThrow(() -> new IllegalStateException("Bay not found"));
        }

        locked.setStatus("OCCUPIED");
        locked.setCurrentVehicleNumber(vehicleNumber);

        ParkingBooking booking = new ParkingBooking();
        booking.setCommunityId(communityId);
        booking.setBayId(locked.getId());
        booking.setVehicleNumber(vehicleNumber);
        booking.setVehicleType(vehicleType);
        booking.setVisitorName(visitorName);
        booking.setHostFlat(hostFlat);
        booking.setBookingType("VISITOR");
        booking.setStatus("ACTIVE");
        booking.setCheckInTime(Instant.now());
        booking.setAssignedBasement(locked.getBasement());
        booking.setAssignedBayNumber(locked.getBayNumber());

        ParkingBooking saved = bookingRepository.save(booking);
        locked.setCurrentBookingId(saved.getId());
        bayRepository.save(locked);

        long remaining = bayRepository.countAvailableVisitorBays(communityId);
        log.info("Visitor parking allocated: {} → {}-{} for {}", vehicleNumber,
                locked.getBasement(), locked.getBayNumber(), visitorName);

        return new AllocationResult(true, locked.getBasement(), locked.getBayNumber(),
                saved.getId(), "Allocated " + locked.getBasement() + "-" + locked.getBayNumber(), remaining);
    }

    @Transactional
    public ReleaseResult releaseParking(Long communityId, String vehicleNumber) {
        ParkingBay bay = bayRepository.findByCurrentVehicle(communityId, vehicleNumber)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found in parking"));

        ParkingBooking booking = bookingRepository.findActiveByVehicle(communityId, vehicleNumber)
                .orElse(null);

        bay.setStatus("AVAILABLE");
        bay.setCurrentVehicleNumber(null);
        bay.setCurrentBookingId(null);
        bayRepository.save(bay);

        Duration duration = Duration.ZERO;
        if (booking != null) {
            booking.setStatus("COMPLETED");
            booking.setCheckOutTime(Instant.now());
            duration = Duration.between(booking.getCheckInTime(), Instant.now());
            bookingRepository.save(booking);
        }

        log.info("Parking released: {} from {}-{} ({}min)", vehicleNumber,
                bay.getBasement(), bay.getBayNumber(), duration.toMinutes());

        return new ReleaseResult(bay.getBasement(), bay.getBayNumber(),
                duration.toMinutes(), "Released successfully");
    }

    public ParkingStatus getStatus(Long communityId) {
        long totalVisitor = bayRepository.findByCommunityIdAndBayType(communityId, "VISITOR").size();
        long available = bayRepository.countAvailableVisitorBays(communityId);
        List<Object[]> byBasement = bayRepository.countAvailableByBasement(communityId);
        Map<String, Long> basementAvailability = byBasement.stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1]));
        long activeBookings = bookingRepository.countActiveBookings(communityId);

        return new ParkingStatus(totalVisitor, available, totalVisitor - available,
                activeBookings, basementAvailability);
    }

    private String mapVehicleClearance(String vehicleType) {
        return switch (vehicleType.toUpperCase()) {
            case "SUV", "MUV", "TRUCK" -> "SUV";
            default -> "SEDAN";
        };
    }

    public record AllocationResult(boolean allocated, String basement, String bayNumber,
                                     Long bookingId, String message, long remainingSpots) {}

    public record ReleaseResult(String basement, String bayNumber, long durationMinutes, String message) {}

    public record ParkingStatus(long totalVisitorBays, long available, long occupied,
                                 long activeBookings, Map<String, Long> availableByBasement) {}
}
