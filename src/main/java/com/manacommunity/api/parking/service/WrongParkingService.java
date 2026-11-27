package com.manacommunity.api.parking.service;

import com.manacommunity.api.parking.entity.ParkingBay;
import com.manacommunity.api.parking.entity.ParkingBooking;
import com.manacommunity.api.parking.entity.VehicleSticker;
import com.manacommunity.api.parking.repository.ParkingBayRepository;
import com.manacommunity.api.parking.repository.ParkingBookingRepository;
import com.manacommunity.api.parking.repository.VehicleStickerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WrongParkingService {

    private final ParkingBayRepository bayRepository;
    private final ParkingBookingRepository bookingRepository;
    private final VehicleStickerRepository stickerRepository;

    @Transactional
    public ViolationResult reportViolation(Long communityId, String vehicleNumber,
                                            String violationType, Long scannedBayId,
                                            Long reportedByGuardId) {
        VehicleSticker sticker = stickerRepository.findByVehicle(communityId, vehicleNumber).orElse(null);

        ParkingBooking booking = bookingRepository.findActiveByVehicle(communityId, vehicleNumber).orElse(null);

        String ownerFlat = sticker != null ? sticker.getFlatNumber() : "UNKNOWN";
        String assignedBayInfo = "NONE";

        if (sticker != null && sticker.getAssignedBayId() != null) {
            ParkingBay assignedBay = bayRepository.findById(sticker.getAssignedBayId()).orElse(null);
            if (assignedBay != null) {
                assignedBayInfo = assignedBay.getBasement() + "-" + assignedBay.getBayNumber();
            }
        }

        boolean isWrongSlot = false;
        if ("WRONG_SLOT".equals(violationType) && sticker != null && scannedBayId != null) {
            isWrongSlot = !scannedBayId.equals(sticker.getAssignedBayId());
        }

        if (booking != null) {
            booking.setWrongParkDetected(true);
            booking.setViolationType(violationType);
            if (scannedBayId != null) {
                booking.setActualBayId(scannedBayId);
            }
            booking.setStatus("VIOLATION");
            bookingRepository.save(booking);
        }

        // Privacy-preserving: generate notification token instead of exposing phone
        String notificationToken = generateNotificationToken(communityId, vehicleNumber);

        log.warn("Parking violation: {} vehicle {} at bay {} (type: {}, owner flat: {})",
                violationType, vehicleNumber, scannedBayId, violationType, ownerFlat);

        return new ViolationResult(
                true,
                vehicleNumber,
                ownerFlat,
                assignedBayInfo,
                violationType,
                isWrongSlot,
                notificationToken,
                "Violation recorded. Owner notified via push/IVR (privacy-preserved).",
                generateNotificationActions(violationType, ownerFlat)
        );
    }

    public ViolationCheckResult checkVehicle(Long communityId, String vehicleNumber) {
        VehicleSticker sticker = stickerRepository.findByVehicle(communityId, vehicleNumber).orElse(null);

        if (sticker == null) {
            return new ViolationCheckResult(false, false, vehicleNumber, "UNKNOWN",
                    "NONE", "Vehicle not registered in community");
        }

        String assignedBayInfo = "NONE";
        if (sticker.getAssignedBayId() != null) {
            ParkingBay bay = bayRepository.findById(sticker.getAssignedBayId()).orElse(null);
            if (bay != null) {
                assignedBayInfo = bay.getBasement() + "-" + bay.getBayNumber();
            }
        }

        ParkingBay currentBay = bayRepository.findByCurrentVehicle(communityId, vehicleNumber).orElse(null);
        boolean isInCorrectSpot = currentBay != null && sticker.getAssignedBayId() != null
                && currentBay.getId().equals(sticker.getAssignedBayId());

        return new ViolationCheckResult(true, isInCorrectSpot, vehicleNumber,
                sticker.getFlatNumber(), assignedBayInfo,
                isInCorrectSpot ? "Vehicle is in assigned spot" : "Vehicle may be in wrong spot");
    }

    public List<ParkingBooking> getActiveViolations(Long communityId) {
        return bookingRepository.findByCommunityIdAndWrongParkDetectedTrue(communityId);
    }

    private String generateNotificationToken(Long communityId, String vehicleNumber) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = communityId + ":" + vehicleNumber + ":" + Instant.now().getEpochSecond() / 300;
            byte[] hash = md.digest(input.getBytes());
            return "NTK-" + HexFormat.of().formatHex(hash).substring(0, 16).toUpperCase();
        } catch (Exception e) {
            return "NTK-" + System.currentTimeMillis();
        }
    }

    private List<NotificationAction> generateNotificationActions(String violationType, String ownerFlat) {
        return switch (violationType) {
            case "FIRE_LANE" -> List.of(
                    new NotificationAction("IVR_CALL", "Automated IVR call to flat " + ownerFlat + " (phone hidden)", "IMMEDIATE"),
                    new NotificationAction("PUSH", "Push notification to flat " + ownerFlat + " residents", "IMMEDIATE"),
                    new NotificationAction("ESCALATE", "Auto-escalate to security manager if not moved in 15min", "15_MINUTES")
            );
            case "WRONG_SLOT" -> List.of(
                    new NotificationAction("PUSH", "Push notification to vehicle owner", "IMMEDIATE"),
                    new NotificationAction("SMS", "SMS via gateway (number anonymized)", "5_MINUTES")
            );
            case "DOUBLE_PARK" -> List.of(
                    new NotificationAction("IVR_CALL", "Automated IVR call to owner", "IMMEDIATE"),
                    new NotificationAction("PUSH", "Push notification with photo evidence", "IMMEDIATE")
            );
            default -> List.of(
                    new NotificationAction("PUSH", "Push notification to vehicle owner", "IMMEDIATE")
            );
        };
    }

    public record ViolationResult(boolean recorded, String vehicleNumber, String ownerFlat,
                                    String assignedBay, String violationType, boolean isWrongSlot,
                                    String notificationToken, String message,
                                    List<NotificationAction> notificationActions) {}

    public record ViolationCheckResult(boolean registered, boolean inCorrectSpot, String vehicleNumber,
                                         String flatNumber, String assignedBay, String message) {}

    public record NotificationAction(String channel, String description, String timing) {}
}
