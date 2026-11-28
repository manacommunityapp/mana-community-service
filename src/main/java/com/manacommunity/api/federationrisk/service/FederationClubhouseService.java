package com.manacommunity.api.federationrisk.service;

import com.manacommunity.api.federationrisk.entity.FederationSubSociety;
import com.manacommunity.api.federationrisk.repository.FederationSubSocietyRepository;
import com.manacommunity.api.federationrisk.service.FederationGateRoutingService.CrossSocietyIsolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates bookings, per-flat amenity quotas, peak-hour restrictions, and wallet
 * billing for a shared central federation clubhouse used across independent sub-societies
 * (e.g., Phase 1 and Phase 2 of a mega-township).
 */
@Service
@RequiredArgsConstructor
public class FederationClubhouseService {

    private final FederationSubSocietyRepository subSocietyRepo;

    // Key: federationId:societyId:flatNo:amenityCode -> bookings used
    private final Map<String, Integer> flatQuotaUsage = new ConcurrentHashMap<>();
    // Key: federationId:societyId:flatNo:amenityCode -> active peak bookings
    private final Map<String, Integer> peakUsage = new ConcurrentHashMap<>();
    // Key: federationId:societyId:flatNo -> wallet balance in paise
    private final Map<String, Long> phaseWalletsPaise = new ConcurrentHashMap<>();

    public record CrossPhaseBookingRequest(
            long federationId,
            long residentSocietyId,
            String flatNo,
            String amenityCode,
            boolean peakHour,
            int maxWeeklyQuota,
            int maxConcurrentPeakBookings,
            long feePaise
    ) {}

    public record CrossPhaseBookingResult(
            boolean confirmed,
            String bookingRef,
            long billedSocietyId,
            long chargedPaise,
            long remainingWalletPaise,
            String rejectionReason
    ) {}

    public void seedPhaseWallet(long federationId, long societyId, String flatNo, long balancePaise) {
        phaseWalletsPaise.put(walletKey(federationId, societyId, flatNo), balancePaise);
    }

    public CrossPhaseBookingResult bookSharedAmenity(CrossPhaseBookingRequest req) {
        FederationSubSociety phase = subSocietyRepo
                .findByFederationIdAndSocietyId(req.federationId(), req.residentSocietyId())
                .orElseThrow(() -> new CrossSocietyIsolationException(
                        "Society " + req.residentSocietyId() + " is not enrolled in federation " + req.federationId()));

        String qKey = quotaKey(req.federationId(), req.residentSocietyId(), req.flatNo(), req.amenityCode());
        String wKey = walletKey(req.federationId(), req.residentSocietyId(), req.flatNo());

        int used = flatQuotaUsage.getOrDefault(qKey, 0);
        if (used >= req.maxWeeklyQuota()) {
            return new CrossPhaseBookingResult(
                    false, null, phase.getSocietyId(), 0L, phaseWalletsPaise.getOrDefault(wKey, 0L),
                    "Weekly amenity quota exceeded for " + phase.getPhaseLabel() + " flat " + req.flatNo());
        }

        int currentPeak = peakUsage.getOrDefault(qKey, 0);
        if (req.peakHour() && currentPeak >= req.maxConcurrentPeakBookings()) {
            return new CrossPhaseBookingResult(
                    false, null, phase.getSocietyId(), 0L, phaseWalletsPaise.getOrDefault(wKey, 0L),
                    "Peak-hour concurrent booking limit reached for " + req.amenityCode());
        }

        long walletBalance = phaseWalletsPaise.getOrDefault(wKey, 0L);
        if (walletBalance < req.feePaise()) {
            return new CrossPhaseBookingResult(
                    false, null, phase.getSocietyId(), 0L, walletBalance,
                    "Insufficient phase wallet balance for flat " + req.flatNo());
        }

        long newBalance = walletBalance - req.feePaise();
        phaseWalletsPaise.put(wKey, newBalance);
        flatQuotaUsage.put(qKey, used + 1);
        if (req.peakHour()) {
            peakUsage.put(qKey, currentPeak + 1);
        }

        String ref = "FED-CLUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new CrossPhaseBookingResult(true, ref, phase.getSocietyId(), req.feePaise(), newBalance, null);
    }

    private static String quotaKey(long fedId, long socId, String flatNo, String amenity) {
        return fedId + ":" + socId + ":" + flatNo + ":" + amenity;
    }

    private static String walletKey(long fedId, long socId, String flatNo) {
        return fedId + ":" + socId + ":" + flatNo;
    }
}
