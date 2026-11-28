package com.manacommunity.api.communityops.parcel;

import com.manacommunity.api.notification.orchestrator.NotificationDtos.UnifiedNotificationRequest;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.NotificationCategory;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.NotificationPriority;
import com.manacommunity.api.notification.orchestrator.UnifiedNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParcelService {

    private final ParcelLogRepository repo;
    private final ObjectProvider<UnifiedNotificationService> notificationServiceProvider;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public ParcelLog receive(Long communityId, Long guardUserId, ParcelReceiveRequest req) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        ParcelLog parcel = repo.save(ParcelLog.builder()
            .communityId(communityId)
            .flatNumber(req.flatNumber())
            .residentUserId(req.residentUserId())
            .senderName(req.senderName())
            .courierName(req.courierName())
            .trackingNumber(req.trackingNumber())
            .description(req.description())
            .status(ParcelStatus.RECEIVED)
            .receivedBy(guardUserId)
            .receivedAt(Instant.now())
            .pickupOtp(otp)
            .notes(req.notes())
            .build());
        log.info("[Parcel] received id={} flat={} otp={}", parcel.getId(), req.flatNumber(), otp);

        // Dispatch OTP pickup notification to resident
        if (parcel.getResidentUserId() != null) {
            try {
                UnifiedNotificationService notif = notificationServiceProvider.getIfAvailable();
                if (notif != null) {
                    notif.orchestrate(UnifiedNotificationRequest.builder()
                            .recipientUserId(parcel.getResidentUserId())
                            .category(NotificationCategory.GATE_ACCESS)
                            .priority(NotificationPriority.HIGH)
                            .title("Parcel Arrived at Gate")
                            .body(String.format("Parcel from %s (%s) is at the gate. Show OTP %s to collect.",
                                    req.courierName() != null ? req.courierName() : "Courier",
                                    req.senderName() != null ? req.senderName() : "Sender",
                                    otp))
                            .data(Map.of("parcelId", parcel.getId(), "otp", otp, "flatNumber", req.flatNumber()))
                            .actionUrl("/resident/parcels/" + parcel.getId())
                            .build());
                    parcel.setStatus(ParcelStatus.NOTIFIED);
                    parcel = repo.save(parcel);
                }
            } catch (Exception e) {
                log.warn("[Parcel] Failed to dispatch pickup notification for parcel {}: {}", parcel.getId(), e.getMessage());
            }
        }
        return parcel;
    }

    @Transactional
    public ParcelLog acknowledge(Long parcelId, Long residentUserId, String otp) {
        ParcelLog parcel = repo.findById(parcelId)
            .orElseThrow(() -> new IllegalArgumentException("Parcel not found"));
        if (!otp.equals(parcel.getPickupOtp())) throw new IllegalArgumentException("Invalid OTP");
        if (parcel.getStatus() == ParcelStatus.PICKED_UP) throw new IllegalStateException("Already picked up");
        parcel.setStatus(ParcelStatus.PICKED_UP);
        parcel.setPickedUpBy(residentUserId);
        parcel.setPickedUpAt(Instant.now());
        return repo.save(parcel);
    }

    public List<ParcelLog> getPendingForCommunity(Long communityId) {
        return repo.findByCommunityIdAndStatus(communityId, ParcelStatus.RECEIVED);
    }

    public List<ParcelLog> getPendingForFlat(Long communityId, String flatNumber) {
        return repo.findByCommunityIdAndFlatNumberAndStatus(communityId, flatNumber, ParcelStatus.RECEIVED);
    }

    public List<ParcelLog> getMyParcels(Long residentUserId) {
        return repo.findByResidentUserIdAndStatusIn(residentUserId, List.of(ParcelStatus.RECEIVED, ParcelStatus.NOTIFIED));
    }
}
