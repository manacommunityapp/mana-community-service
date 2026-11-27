package com.manacommunity.api.parking.service;

import com.manacommunity.api.parking.entity.EvCharger;
import com.manacommunity.api.parking.entity.EvChargingSession;
import com.manacommunity.api.parking.repository.EvChargerRepository;
import com.manacommunity.api.parking.repository.EvChargingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvChargerQueueService {

    private final EvChargerRepository chargerRepository;
    private final EvChargingSessionRepository sessionRepository;

    @Transactional
    public ReservationResult reserveCharger(Long communityId, String flatNumber, Long userId,
                                              String vehicleNumber, Long preferredChargerId) {
        EvCharger charger = null;

        if (preferredChargerId != null) {
            charger = chargerRepository.findByIdWithLock(preferredChargerId)
                    .filter(c -> "AVAILABLE".equals(c.getStatus()))
                    .orElse(null);
        }

        if (charger == null) {
            List<EvCharger> available = chargerRepository.findAvailableChargers(communityId);
            if (!available.isEmpty()) {
                charger = chargerRepository.findByIdWithLock(available.get(0).getId())
                        .filter(c -> "AVAILABLE".equals(c.getStatus()))
                        .orElse(null);
            }
        }

        if (charger != null) {
            EvChargingSession session = new EvChargingSession();
            session.setCommunityId(communityId);
            session.setChargerId(charger.getId());
            session.setFlatNumber(flatNumber);
            session.setUserId(userId);
            session.setVehicleNumber(vehicleNumber);
            session.setStatus("CHARGING");
            session.setStartTime(Instant.now());
            session.setMaxSessionMinutes(charger.getMaxSessionMinutes());

            EvChargingSession saved = sessionRepository.save(session);

            charger.setStatus("CHARGING");
            charger.setCurrentSessionId(saved.getId());
            chargerRepository.save(charger);

            log.info("EV charging started: charger {} for flat {} vehicle {}", charger.getChargerCode(), flatNumber, vehicleNumber);
            return new ReservationResult(true, saved.getId(), charger.getChargerCode(),
                    0, "Charging started on " + charger.getChargerCode());
        }

        // Queue the request
        List<EvChargingSession> queue = sessionRepository.findAllQueued(communityId);
        int position = queue.size() + 1;

        EvChargingSession queued = new EvChargingSession();
        queued.setCommunityId(communityId);
        queued.setChargerId(0L);
        queued.setFlatNumber(flatNumber);
        queued.setUserId(userId);
        queued.setVehicleNumber(vehicleNumber);
        queued.setStatus("QUEUED");
        queued.setQueuePosition(position);

        EvChargingSession saved = sessionRepository.save(queued);
        log.info("EV charge queued: position {} for flat {} vehicle {}", position, flatNumber, vehicleNumber);

        return new ReservationResult(false, saved.getId(), null, position,
                "Queued at position " + position + " (est. wait: " + (position * 60) + " min)");
    }

    @Transactional
    public SessionEndResult endSession(Long sessionId) {
        EvChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        session.setEndTime(Instant.now());
        Duration duration = Duration.between(session.getStartTime(), Instant.now());

        EvCharger charger = chargerRepository.findById(session.getChargerId()).orElse(null);

        BigDecimal energyKwh = charger != null
                ? charger.getPowerKw().multiply(BigDecimal.valueOf(duration.toMinutes())).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal energyCost = energyKwh.multiply(charger != null ? charger.getRatePerKwh() : BigDecimal.valueOf(12));

        int overstayMinutes = 0;
        BigDecimal overstayPenalty = BigDecimal.ZERO;
        if (session.getMaxSessionMinutes() != null && duration.toMinutes() > session.getMaxSessionMinutes()) {
            overstayMinutes = (int) (duration.toMinutes() - session.getMaxSessionMinutes());
            BigDecimal penaltyRate = charger != null ? charger.getOverstayPenaltyPerHour() : BigDecimal.valueOf(50);
            overstayPenalty = penaltyRate.multiply(BigDecimal.valueOf(overstayMinutes))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            session.setStatus("OVERSTAY");
        } else {
            session.setStatus("COMPLETED");
        }

        session.setEnergyKwh(energyKwh);
        session.setEnergyCost(energyCost);
        session.setOverstayMinutes(overstayMinutes);
        session.setOverstayPenalty(overstayPenalty);
        session.setTotalAmount(energyCost.add(overstayPenalty));
        sessionRepository.save(session);

        if (charger != null) {
            charger.setStatus("AVAILABLE");
            charger.setCurrentSessionId(null);
            chargerRepository.save(charger);
            promoteQueue(session.getCommunityId(), charger.getId());
        }

        log.info("EV session ended: {} kWh, cost ₹{}, overstay {}min penalty ₹{}",
                energyKwh, energyCost, overstayMinutes, overstayPenalty);

        return new SessionEndResult(session.getId(), energyKwh, energyCost,
                overstayMinutes, overstayPenalty, session.getTotalAmount(), session.getStatus());
    }

    @Transactional
    public List<AutoCutoffResult> runAutoCutoff(Long communityId) {
        Instant cutoff = Instant.now().minus(Duration.ofHours(4));
        List<EvChargingSession> overdue = sessionRepository.findOverdueSessions(communityId, cutoff);
        List<AutoCutoffResult> results = new ArrayList<>();

        for (EvChargingSession session : overdue) {
            SessionEndResult end = endSession(session.getId());
            results.add(new AutoCutoffResult(session.getId(), session.getFlatNumber(),
                    session.getVehicleNumber(), end.totalAmount()));
            log.warn("Auto-cutoff: session {} flat {} after max session exceeded", session.getId(), session.getFlatNumber());
        }
        return results;
    }

    private void promoteQueue(Long communityId, Long chargerId) {
        List<EvChargingSession> queue = sessionRepository.findAllQueued(communityId);
        if (queue.isEmpty()) return;

        EvChargingSession next = queue.get(0);
        next.setChargerId(chargerId);
        next.setStatus("CHARGING");
        next.setStartTime(Instant.now());
        next.setQueuePosition(null);
        sessionRepository.save(next);

        EvCharger charger = chargerRepository.findById(chargerId).orElse(null);
        if (charger != null) {
            charger.setStatus("CHARGING");
            charger.setCurrentSessionId(next.getId());
            chargerRepository.save(charger);
        }

        for (int i = 1; i < queue.size(); i++) {
            queue.get(i).setQueuePosition(i);
            sessionRepository.save(queue.get(i));
        }

        log.info("Queue promoted: session {} now charging on charger {}", next.getId(), chargerId);
    }

    public List<EvChargingSession> getUnbilledSessions(Long communityId, String flatNumber) {
        return sessionRepository.findUnbilledByFlat(communityId, flatNumber);
    }

    public List<EvCharger> getChargers(Long communityId) {
        return chargerRepository.findByCommunityId(communityId);
    }

    public record ReservationResult(boolean startedCharging, Long sessionId, String chargerCode,
                                      int queuePosition, String message) {}

    public record SessionEndResult(Long sessionId, BigDecimal energyKwh, BigDecimal energyCost,
                                     int overstayMinutes, BigDecimal overstayPenalty,
                                     BigDecimal totalAmount, String status) {}

    public record AutoCutoffResult(Long sessionId, String flatNumber, String vehicleNumber, BigDecimal amount) {}
}
