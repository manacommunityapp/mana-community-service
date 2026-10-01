package com.manacommunity.api.parking.ev.service;

import com.manacommunity.api.cfbos.shared.exception.CfbosException;
import com.manacommunity.api.cfbos.wallet.engine.WalletEngine;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWallet;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWalletTransaction;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.parking.entity.ResidentVehicle;
import com.manacommunity.api.parking.ev.dto.*;
import com.manacommunity.api.parking.ev.engine.EvBillingEngine;
import com.manacommunity.api.parking.ev.entity.EvCharger;
import com.manacommunity.api.parking.ev.entity.EvChargingRate;
import com.manacommunity.api.parking.ev.entity.EvChargingSession;
import com.manacommunity.api.parking.ev.entity.EvTelemetryHeartbeat;
import com.manacommunity.api.parking.ev.repository.EvChargerRepository;
import com.manacommunity.api.parking.ev.repository.EvChargingRateRepository;
import com.manacommunity.api.parking.ev.repository.EvChargingSessionRepository;
import com.manacommunity.api.parking.ev.repository.EvTelemetryHeartbeatRepository;
import com.manacommunity.api.parking.repository.ResidentVehicleRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvChargingService {

    private final EvChargerRepository chargerRepository;
    private final EvChargingRateRepository rateRepository;
    private final EvChargingSessionRepository sessionRepository;
    private final EvTelemetryHeartbeatRepository heartbeatRepository;
    private final ResidentVehicleRepository vehicleRepository;
    private final WalletEngine walletEngine;
    private final EvBillingEngine billingEngine;

    // ── Session Lifecycle ─────────────────────────────────────────────────────

    @Transactional
    public EvChargingSessionResponse startSession(AppUser resident, StartChargingRequest request) {
        EvCharger charger = chargerRepository.findById(request.chargerId())
                .orElseThrow(() -> new ResourceNotFoundException("EvCharger", request.chargerId()));

        if (charger.getStatus() != EvCharger.ChargerStatus.AVAILABLE) {
            throw new InvalidInputException("Charger is currently " + charger.getStatus() + " and cannot start a new session");
        }

        // Check active resident session
        sessionRepository.findFirstByResidentIdAndStatus(resident.getId(), EvChargingSession.SessionStatus.ACTIVE)
                .ifPresent(s -> {
                    throw new InvalidInputException("Resident already has an active EV charging session (ID: " + s.getId() + ")");
                });

        // Validate wallet balance against minimum required charge
        EvChargingRate rateConfig = rateRepository.findFirstByCommunityIdAndActiveTrue(resident.getCommunity().getId())
                .orElse(null);
        BigDecimal minBalanceRequired = rateConfig != null ? rateConfig.getMinimumBillAmount() : new BigDecimal("20.00");

        CfbosWallet wallet = walletEngine.getOrCreateWallet(resident.getId());
        if (wallet.getBalance().compareTo(minBalanceRequired) < 0) {
            throw new CfbosException("Insufficient wallet balance to start charging. Minimum balance required: " + minBalanceRequired + ", Available: " + wallet.getBalance());
        }

        ResidentVehicle vehicle = null;
        if (request.vehicleId() != null) {
            vehicle = vehicleRepository.findById(request.vehicleId()).orElse(null);
        }

        double startMeter = request.currentMeterReading() != null ? request.currentMeterReading() : (charger.getLatestMeterKwh() != null ? charger.getLatestMeterKwh() : 0.0);

        EvChargingSession session = EvChargingSession.builder()
                .community(resident.getCommunity())
                .charger(charger)
                .resident(resident)
                .vehicle(vehicle)
                .startedAt(LocalDateTime.now())
                .startMeterKwh(startMeter)
                .status(EvChargingSession.SessionStatus.ACTIVE)
                .build();

        session = sessionRepository.save(session);

        charger.setStatus(EvCharger.ChargerStatus.CHARGING);
        chargerRepository.save(charger);

        log.info("EV Charging session started: id={} resident={} charger={}",
                session.getId(), resident.getFullName(), charger.getDeviceId());

        return EvChargingSessionResponse.from(session);
    }

    @Transactional
    public EvChargingSessionResponse stopSession(AppUser resident, Long sessionId, StopChargingRequest request) {
        EvChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("EvChargingSession", sessionId));

        if (session.getStatus() != EvChargingSession.SessionStatus.ACTIVE) {
            throw new InvalidInputException("Session is not active (current status: " + session.getStatus() + ")");
        }

        LocalDateTime endedAt = LocalDateTime.now();
        double endMeter = request.finalMeterReading() != null ? request.finalMeterReading() : session.getStartMeterKwh();
        double totalKwh = billingEngine.calculateKwh(session.getStartMeterKwh(), endMeter);

        EvChargingRate rateConfig = rateRepository.findFirstByCommunityIdAndActiveTrue(session.getCommunity().getId())
                .orElse(null);

        BigDecimal totalCost = billingEngine.calculateCost(totalKwh, session.getStartedAt(), endedAt, rateConfig);

        // Debit resident's CFBOS wallet
        String narration = "EV Charging Session #" + session.getId() + " (" + totalKwh + " kWh @ Slot " +
                (session.getCharger().getParkingSlot() != null ? session.getCharger().getParkingSlot().getSlotNumber() : "N/A") + ")";

        CfbosWalletTransaction txn = walletEngine.debitWallet(
                session.getResident().getId(),
                totalCost,
                WalletTransactionType.EV_CHARGING,
                "EV_CHARGING",
                session.getId(),
                narration
        );

        session.setEndedAt(endedAt);
        session.setEndMeterKwh(endMeter);
        session.setTotalKwh(totalKwh);
        session.setTotalCost(totalCost);
        session.setStatus(EvChargingSession.SessionStatus.COMPLETED);
        session.setCfbosWalletTransactionId(txn != null ? txn.getId() : null);
        session.setStopReason(request.stopReason() != null ? request.stopReason() : "USER_STOP");

        session = sessionRepository.save(session);

        // Reset charger status
        EvCharger charger = session.getCharger();
        charger.setStatus(EvCharger.ChargerStatus.AVAILABLE);
        charger.setCurrentPowerKw(BigDecimal.ZERO);
        charger.setLatestMeterKwh(endMeter);
        chargerRepository.save(charger);

        log.info("EV Charging session completed: id={} kwh={} cost={} txnId={}",
                session.getId(), totalKwh, totalCost, txn != null ? txn.getId() : null);

        return EvChargingSessionResponse.from(session);
    }

    // ── Telemetry Ingestion ───────────────────────────────────────────────────

    @Transactional
    public void processTelemetry(EvTelemetryRequest request) {
        EvCharger charger = chargerRepository.findByDeviceId(request.deviceId())
                .orElseThrow(() -> new ResourceNotFoundException("No EV charger found with deviceId: " + request.deviceId()));

        Optional<EvChargingSession> activeSessionOpt = sessionRepository
                .findFirstByChargerIdAndStatus(charger.getId(), EvChargingSession.SessionStatus.ACTIVE);

        Long sessionId = activeSessionOpt.map(EvChargingSession::getId).orElse(null);

        // Save Heartbeat Log
        EvTelemetryHeartbeat heartbeat = EvTelemetryHeartbeat.builder()
                .chargerId(charger.getId())
                .sessionId(sessionId)
                .timestamp(request.timestamp() != null ? request.timestamp() : LocalDateTime.now())
                .powerKw(request.powerKw() != null ? request.powerKw() : BigDecimal.ZERO)
                .voltage(request.voltage())
                .currentAmps(request.currentAmps())
                .temperatureCelsius(request.temperatureCelsius())
                .meterReadingKwh(request.meterReadingKwh())
                .stateOfChargePercent(request.stateOfChargePercent())
                .errorCode(request.errorCode())
                .build();

        heartbeatRepository.save(heartbeat);

        // Update live metrics on Charger
        charger.setLatestMeterKwh(request.meterReadingKwh());
        if (request.powerKw() != null) {
            charger.setCurrentPowerKw(request.powerKw());
        }
        charger.setLastHeartbeatAt(request.timestamp() != null ? request.timestamp() : LocalDateTime.now());
        charger.setLastFaultCode(request.errorCode());

        // Evaluate Status
        if (request.errorCode() != null && !request.errorCode().isBlank()) {
            charger.setStatus(EvCharger.ChargerStatus.FAULT);
        } else if (activeSessionOpt.isPresent()) {
            BigDecimal p = request.powerKw() != null ? request.powerKw() : BigDecimal.ZERO;
            if (p.compareTo(new BigDecimal("0.05")) > 0) {
                charger.setStatus(EvCharger.ChargerStatus.CHARGING);
            } else {
                charger.setStatus(EvCharger.ChargerStatus.OCCUPIED_IDLE);
            }
        } else {
            charger.setStatus(EvCharger.ChargerStatus.AVAILABLE);
        }

        chargerRepository.save(charger);
    }

    @Transactional(readOnly = true)
    public EvLiveTelemetryResponse getLiveTelemetry(Long chargerId) {
        EvCharger charger = chargerRepository.findById(chargerId)
                .orElseThrow(() -> new ResourceNotFoundException("EvCharger", chargerId));

        List<EvTelemetryHeartbeat> recent = heartbeatRepository.findTop20ByChargerIdOrderByTimestampDesc(chargerId);
        EvTelemetryHeartbeat latest = recent.isEmpty() ? null : recent.get(0);

        return new EvLiveTelemetryResponse(
                charger.getId(),
                charger.getDeviceId(),
                charger.getStatus(),
                charger.getCurrentPowerKw(),
                latest != null ? latest.getVoltage() : null,
                latest != null ? latest.getCurrentAmps() : null,
                latest != null ? latest.getTemperatureCelsius() : null,
                charger.getLatestMeterKwh(),
                latest != null ? latest.getStateOfChargePercent() : null,
                charger.getLastHeartbeatAt()
        );
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EvChargingSessionResponse> getMySessions(Long residentId) {
        return sessionRepository.findByResidentIdOrderByStartedAtDesc(residentId)
                .stream()
                .map(EvChargingSessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<EvChargingSessionResponse> getCommunitySessions(Long communityId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("startedAt").descending());
        return sessionRepository.findByCommunityIdOrderByStartedAtDesc(communityId, pageable)
                .map(EvChargingSessionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<EvChargerResponse> getChargers(Long communityId) {
        return chargerRepository.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream()
                .map(EvChargerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EvChargerResponse getCharger(Long chargerId) {
        EvCharger charger = chargerRepository.findById(chargerId)
                .orElseThrow(() -> new ResourceNotFoundException("EvCharger", chargerId));
        return EvChargerResponse.from(charger);
    }
}
