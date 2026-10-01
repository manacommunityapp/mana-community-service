package com.manacommunity.api.parking.ev.service.impl;

import com.manacommunity.api.cfbos.shared.exception.CfbosException;
import com.manacommunity.api.cfbos.wallet.engine.WalletEngine;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWallet;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWalletTransaction;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.parking.ev.dto.*;
import com.manacommunity.api.parking.ev.engine.EvChargingBillingEngine;
import com.manacommunity.api.parking.ev.engine.EvTelemetryEngine;
import com.manacommunity.api.parking.ev.entity.*;
import com.manacommunity.api.parking.ev.enums.*;
import com.manacommunity.api.parking.ev.repository.*;
import com.manacommunity.api.parking.ev.service.EvChargingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvChargingServiceImpl implements EvChargingService {

    private final EvChargingStationRepository stationRepository;
    private final EvChargingSessionRepository sessionRepository;
    private final EvTelemetryHeartbeatRepository heartbeatRepository;
    private final EvTariffConfigRepository tariffRepository;
    private final EvTelemetryEngine telemetryEngine;
    private final EvChargingBillingEngine billingEngine;
    private final WalletEngine walletEngine;

    @Override
    @Transactional
    public EvStationResponse registerStation(EvStationResponse req) {
        EvChargingStation station = EvChargingStation.builder()
                .hardwareId(req.hardwareId())
                .name(req.name())
                .communityId(req.communityId())
                .parkingSlotId(req.parkingSlotId())
                .connectorType(req.connectorType() != null ? req.connectorType() : EvConnectorType.TYPE_2_AC)
                .maxPowerKw(req.maxPowerKw() != null ? req.maxPowerKw() : new BigDecimal("7.40"))
                .status(EvStationStatus.AVAILABLE)
                .currentPowerKw(BigDecimal.ZERO)
                .latestMeterKwh(BigDecimal.ZERO)
                .firmwareVersion(req.firmwareVersion())
                .active(true)
                .build();

        EvChargingStation saved = stationRepository.save(station);
        return toStationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvStationResponse> getCommunityStations(Long communityId) {
        return stationRepository.findByCommunityIdAndActiveTrue(communityId).stream()
                .map(this::toStationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EvStationResponse getStationById(Long stationId) {
        EvChargingStation station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("EV Station not found with id: " + stationId));
        return toStationResponse(station);
    }

    @Override
    @Transactional(readOnly = true)
    public EvLiveTelemetryResponse getLiveTelemetry(Long stationId) {
        EvChargingStation station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("EV Station not found with id: " + stationId));

        List<EvTelemetryHeartbeat> recent = heartbeatRepository.findTop20ByStationIdOrderByTimestampDesc(stationId);
        EvTelemetryHeartbeat latest = recent.isEmpty() ? null : recent.get(0);

        return new EvLiveTelemetryResponse(
                station.getId(),
                station.getHardwareId(),
                station.getStatus(),
                station.getCurrentPowerKw(),
                latest != null ? latest.getVoltage() : null,
                latest != null ? latest.getCurrentAmps() : null,
                latest != null ? latest.getTemperatureCelsius() : null,
                station.getLatestMeterKwh(),
                latest != null ? latest.getStateOfChargePercent() : null,
                station.getLastHeartbeatAt()
        );
    }

    @Override
    @Transactional
    public EvSessionResponse startSession(Long residentId, Long communityId, EvStartSessionRequest req) {
        EvChargingStation station = stationRepository.findById(req.stationId())
                .orElseThrow(() -> new ResourceNotFoundException("EV Station not found with id: " + req.stationId()));

        if (station.getStatus() == EvStationStatus.CHARGING || station.getStatus() == EvStationStatus.OCCUPIED_IDLE) {
            throw new IllegalStateException("EV Station is currently occupied or charging");
        }

        if (station.getStatus() == EvStationStatus.FAULTED || station.getStatus() == EvStationStatus.OFFLINE) {
            throw new IllegalStateException("EV Station is currently unavailable / in fault state");
        }

        BigDecimal initialMeter = req.initialMeterKwh() != null ? req.initialMeterKwh() : station.getLatestMeterKwh();
        LocalDateTime now = LocalDateTime.now();

        EvChargingSession session = EvChargingSession.builder()
                .stationId(station.getId())
                .residentId(residentId)
                .communityId(communityId)
                .vehicleNumber(req.vehicleNumber())
                .startTime(now)
                .initialMeterKwh(initialMeter)
                .status(EvSessionStatus.IN_PROGRESS)
                .paymentStatus(EvPaymentStatus.PENDING)
                .build();

        EvChargingSession savedSession = sessionRepository.save(session);

        station.setStatus(EvStationStatus.CHARGING);
        stationRepository.save(station);

        log.info("Started EV Charging Session #{} on station {} for resident {}", savedSession.getId(), station.getHardwareId(), residentId);
        return toSessionResponse(savedSession);
    }

    @Override
    @Transactional
    public EvSessionResponse stopSession(Long sessionId, Long residentId, EvStopSessionRequest req) {
        EvChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("EV Session not found with id: " + sessionId));

        if (session.getStatus() != EvSessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session is already completed or cancelled");
        }

        LocalDateTime now = LocalDateTime.now();
        session.setEndTime(now);
        session.setFinalMeterKwh(req.finalMeterKwh());
        session.setStopReason(req.stopReason() != null ? req.stopReason() : "USER_REQUEST");

        int durationMin = (int) Duration.between(session.getStartTime(), now).toMinutes();
        session.setChargingDurationMinutes(durationMin);
        session.setIdleDurationMinutes(req.idleDurationMinutes() != null ? req.idleDurationMinutes() : 0);

        // Fetch Tariff and calculate bill
        EvTariffConfig tariff = tariffRepository.findByCommunityIdAndActiveTrue(session.getCommunityId())
                .orElse(null);

        EvChargingBillingEngine.BillBreakdown bill = billingEngine.calculateBill(
                session.getInitialMeterKwh(),
                req.finalMeterKwh(),
                session.getStartTime(),
                now,
                session.getIdleDurationMinutes(),
                tariff
        );

        session.setTotalEnergyKwh(bill.energyKwh());
        session.setEnergyCost(bill.energyCost());
        session.setIdlePenalty(bill.idlePenalty());
        session.setTotalCost(bill.totalCost());
        session.setStatus(EvSessionStatus.COMPLETED);

        // Attempt automated CFBOS Wallet settlement
        if (bill.totalCost().compareTo(BigDecimal.ZERO) > 0) {
            try {
                CfbosWallet wallet = walletEngine.getOrCreateWallet(residentId);
                if (wallet.getBalance().compareTo(bill.totalCost()) >= 0) {
                    CfbosWalletTransaction txn = walletEngine.debitWallet(
                            residentId,
                            bill.totalCost(),
                            WalletTransactionType.EV_CHARGING,
                            "EV_SESSION",
                            session.getId(),
                            "EV Charging Session #" + session.getId() + " (" + bill.energyKwh() + " kWh)"
                    );
                    session.setPaymentStatus(EvPaymentStatus.SETTLED_WALLET);
                    session.setWalletTransactionId(txn.getId());
                    log.info("Settled EV Session #{} via Wallet for amount Rs. {}", session.getId(), bill.totalCost());
                } else {
                    session.setPaymentStatus(EvPaymentStatus.BILLED_UTILITY);
                    log.info("Insufficient wallet balance for EV Session #{}. Flagged for monthly utility billing.", session.getId());
                }
            } catch (Exception e) {
                log.warn("Wallet debit error for EV Session #{}: {}. Falling back to monthly utility billing.", session.getId(), e.getMessage());
                session.setPaymentStatus(EvPaymentStatus.BILLED_UTILITY);
            }
        } else {
            session.setPaymentStatus(EvPaymentStatus.SETTLED_WALLET);
        }

        EvChargingSession saved = sessionRepository.save(session);

        // Free up charging station
        stationRepository.findById(session.getStationId()).ifPresent(st -> {
            st.setStatus(EvStationStatus.AVAILABLE);
            st.setCurrentPowerKw(BigDecimal.ZERO);
            st.setLatestMeterKwh(req.finalMeterKwh());
            stationRepository.save(st);
        });

        return toSessionResponse(saved);
    }

    @Override
    @Transactional
    public void processTelemetry(EvTelemetryRequest req) {
        EvChargingStation station = stationRepository.findByHardwareId(req.hardwareId())
                .orElseThrow(() -> new ResourceNotFoundException("No EV station registered for hardwareId: " + req.hardwareId()));

        Optional<EvChargingSession> activeSessionOpt = sessionRepository
                .findByStationIdAndStatus(station.getId(), EvSessionStatus.IN_PROGRESS);

        Long activeSessionId = activeSessionOpt.map(EvChargingSession::getId).orElse(null);

        // Record Heartbeat
        EvTelemetryHeartbeat heartbeat = telemetryEngine.toHeartbeatEntity(station.getId(), activeSessionId, req);
        heartbeatRepository.save(heartbeat);

        // Update station live metrics
        station.setLatestMeterKwh(req.meterReadingKwh());
        if (req.powerKw() != null) {
            station.setCurrentPowerKw(req.powerKw());
        }
        station.setLastHeartbeatAt(req.timestamp() != null ? req.timestamp() : LocalDateTime.now());
        station.setLastFaultCode(req.errorCode());

        EvStationStatus evaluated = telemetryEngine.evaluateStatus(station, req, activeSessionOpt.isPresent());
        station.setStatus(evaluated);
        stationRepository.save(station);

        // Update session peak power if active
        activeSessionOpt.ifPresent(session -> {
            if (req.powerKw() != null && req.powerKw().compareTo(session.getPeakPowerKw()) > 0) {
                session.setPeakPowerKw(req.powerKw());
                sessionRepository.save(session);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvSessionResponse> getResidentSessions(Long residentId) {
        return sessionRepository.findByResidentIdOrderByStartTimeDesc(residentId).stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvSessionResponse> getCommunitySessions(Long communityId) {
        return sessionRepository.findByCommunityIdOrderByStartTimeDesc(communityId).stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EvSessionResponse getSessionById(Long sessionId) {
        EvChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("EV Session not found with id: " + sessionId));
        return toSessionResponse(session);
    }

    private EvStationResponse toStationResponse(EvChargingStation s) {
        return new EvStationResponse(
                s.getId(),
                s.getHardwareId(),
                s.getName(),
                s.getCommunityId(),
                s.getParkingSlotId(),
                s.getConnectorType(),
                s.getMaxPowerKw(),
                s.getStatus(),
                s.getCurrentPowerKw(),
                s.getLatestMeterKwh(),
                s.getLastHeartbeatAt(),
                s.getFirmwareVersion(),
                s.getLastFaultCode()
        );
    }

    private EvSessionResponse toSessionResponse(EvChargingSession ses) {
        return new EvSessionResponse(
                ses.getId(),
                ses.getStationId(),
                ses.getResidentId(),
                ses.getCommunityId(),
                ses.getVehicleNumber(),
                ses.getStartTime(),
                ses.getEndTime(),
                ses.getInitialMeterKwh(),
                ses.getFinalMeterKwh(),
                ses.getTotalEnergyKwh(),
                ses.getPeakPowerKw(),
                ses.getChargingDurationMinutes(),
                ses.getIdleDurationMinutes(),
                ses.getEnergyCost(),
                ses.getIdlePenalty(),
                ses.getTotalCost(),
                ses.getStatus(),
                ses.getPaymentStatus(),
                ses.getWalletTransactionId(),
                ses.getStopReason()
        );
    }
}
