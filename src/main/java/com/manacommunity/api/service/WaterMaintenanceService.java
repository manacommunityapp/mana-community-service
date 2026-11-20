package com.manacommunity.api.service;

import com.manacommunity.api.dto.WaterDtos.*;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service("waterMaintenanceService")
@RequiredArgsConstructor
public class WaterMaintenanceService {

    private final WaterStorageTankRepository waterStorageTankRepository;
    private final WaterTankerDeliveryRepository waterTankerDeliveryRepository;
    private final WaterPumpRepository waterPumpRepository;
    private final WaterPumpRunLogRepository waterPumpRunLogRepository;
    private final WaterTreatmentLogRepository waterTreatmentLogRepository;
    private final WaterQualityTestRepository waterQualityTestRepository;
    private final TankCleaningScheduleRepository tankCleaningScheduleRepository;

    // ── Storage Tanks & Sumps ──────────────────────────────────────────────

    @Transactional
    public List<WaterStorageTank> getStorageTanks(Long communityId) {
        List<WaterStorageTank> list = waterStorageTankRepository.findByCommunityIdOrderByNameAsc(communityId);
        if (list.isEmpty()) {
            list = seedInitialCommunityTanks(communityId);
        }
        return list;
    }

    public WaterStorageTank getStorageTank(Long communityId, Long id) {
        return waterStorageTankRepository.findByCommunityIdAndId(communityId, id)
                .orElseThrow(() -> new IllegalArgumentException("Water storage tank not found with id: " + id));
    }

    @Transactional
    public WaterStorageTank updateTankLevel(Long communityId, Long id, BigDecimal newLevelLiters) {
        WaterStorageTank tank = getStorageTank(communityId, id);
        BigDecimal clamped = newLevelLiters.max(BigDecimal.ZERO).min(tank.getCapacityLiters());
        tank.setCurrentLevelLiters(clamped);
        tank.setLastTelemetryTime(LocalDateTime.now());
        return waterStorageTankRepository.save(tank);
    }

    // ── Water Tanker Inwarding ──────────────────────────────────────────────

    public List<WaterTankerDelivery> getTankerDeliveries(Long communityId) {
        return waterTankerDeliveryRepository.findByCommunityIdOrderByDeliveryDateDesc(communityId);
    }

    @Transactional
    public WaterTankerDelivery logTankerDelivery(Long communityId, TankerDeliveryRequest req) {
        WaterTankerDelivery delivery = WaterTankerDelivery.builder()
                .communityId(communityId)
                .deliveryDate(LocalDateTime.now())
                .vendorName(req.getVendorName())
                .vehicleNumber(req.getVehicleNumber())
                .capacityLiters(req.getCapacityLiters())
                .cost(req.getCost())
                .invoiceNumber(req.getInvoiceNumber())
                .driverPhone(req.getDriverPhone())
                .targetTankId(req.getTargetTankId())
                .approvedBy(req.getApprovedBy())
                .notes(req.getNotes())
                .status("RECEIVED")
                .build();

        WaterTankerDelivery saved = waterTankerDeliveryRepository.save(delivery);

        // Update target tank level if specified
        if (req.getTargetTankId() != null) {
            waterStorageTankRepository.findByCommunityIdAndId(communityId, req.getTargetTankId())
                    .ifPresent(tank -> {
                        BigDecimal newLevel = tank.getCurrentLevelLiters().add(req.getCapacityLiters())
                                .min(tank.getCapacityLiters());
                        tank.setCurrentLevelLiters(newLevel);
                        tank.setLastTelemetryTime(LocalDateTime.now());
                        waterStorageTankRepository.save(tank);
                    });
        }

        return saved;
    }

    // ── Pump House & HYPN Motors ───────────────────────────────────────────

    @Transactional
    public List<WaterPump> getPumps(Long communityId) {
        List<WaterPump> list = waterPumpRepository.findByCommunityIdOrderByNameAsc(communityId);
        if (list.isEmpty()) {
            list = seedInitialCommunityPumps(communityId);
        }
        return list;
    }

    public WaterPump getPump(Long communityId, Long id) {
        return waterPumpRepository.findByCommunityIdAndId(communityId, id)
                .orElseThrow(() -> new IllegalArgumentException("Pump not found with id: " + id));
    }

    @Transactional
    public WaterPump togglePumpDuty(Long communityId, Long pumpId) {
        WaterPump pump = getPump(communityId, pumpId);
        if ("RUNNING".equalsIgnoreCase(pump.getStatus())) {
            pump.setStatus("STANDBY");
            pump.setIsDutyPump(false);
        } else {
            pump.setStatus("RUNNING");
            pump.setIsDutyPump(true);
        }
        return waterPumpRepository.save(pump);
    }

    @Transactional
    public WaterPumpRunLog logPumpRun(Long communityId, WaterPumpRunRequest req) {
        WaterPump pump = getPump(communityId, req.getPumpId());

        long seconds = Duration.between(req.getStartTime(), req.getEndTime()).getSeconds();
        BigDecimal durationHours = BigDecimal.valueOf(Math.max(seconds, 300))
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);

        WaterPumpRunLog runLog = WaterPumpRunLog.builder()
                .pumpId(pump.getId())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .durationHours(durationHours)
                .flowRateLpm(req.getFlowRateLpm())
                .energyKwh(req.getEnergyKwh())
                .operatorName(req.getOperatorName())
                .notes(req.getNotes())
                .build();

        WaterPumpRunLog saved = waterPumpRunLogRepository.save(runLog);

        // Increment pump total run hours
        pump.setTotalRunHours(pump.getTotalRunHours().add(durationHours));
        waterPumpRepository.save(pump);

        return saved;
    }

    public List<WaterPumpRunLog> getPumpRunLogs(Long pumpId) {
        return waterPumpRunLogRepository.findByPumpIdOrderByStartTimeDesc(pumpId);
    }

    // ── WTP & STP Treatment Operations ─────────────────────────────────────

    public List<WaterTreatmentLog> getTreatmentLogs(Long communityId, String plantType) {
        if (plantType != null && !plantType.trim().isEmpty()) {
            return waterTreatmentLogRepository.findByCommunityIdAndPlantTypeOrderByLogDateDesc(communityId, plantType.toUpperCase());
        }
        return waterTreatmentLogRepository.findByCommunityIdOrderByLogDateDesc(communityId);
    }

    @Transactional
    public WaterTreatmentLog logTreatment(Long communityId, WaterTreatmentLogRequest req) {
        WaterTreatmentLog log = WaterTreatmentLog.builder()
                .communityId(communityId)
                .plantType(req.getPlantType().toUpperCase())
                .operationType(req.getOperationType().toUpperCase())
                .quantityUsedKg(req.getQuantityUsedKg() != null ? req.getQuantityUsedKg() : BigDecimal.ZERO)
                .operatorName(req.getOperatorName())
                .notes(req.getNotes())
                .build();
        return waterTreatmentLogRepository.save(log);
    }

    // ── Water Quality & Hygiene Tests ──────────────────────────────────────

    public List<WaterQualityTest> getQualityTests(Long communityId) {
        return waterQualityTestRepository.findByCommunityIdOrderByTestDateDesc(communityId);
    }

    @Transactional
    public WaterQualityTest logQualityTest(Long communityId, WaterQualityTestRequest req) {
        // Evaluates against BIS IS:10500 Potable Water Specifications
        // pH acceptable range: 6.5 - 8.5
        // TDS acceptable: < 500 ppm (permissible up to 2000 ppm)
        // Hardness: < 300 ppm (permissible up to 600 ppm)
        String status = "SAFE";
        double ph = req.getPh().doubleValue();
        int tds = req.getTdsPpm();
        int hardness = req.getHardnessPpm();

        if (ph < 6.0 || ph > 9.0 || tds > 1200 || hardness > 500) {
            status = "CONTAMINATED";
        } else if (ph < 6.5 || ph > 8.5 || tds > 500 || hardness > 300) {
            status = "MARGINAL";
        }

        WaterQualityTest test = WaterQualityTest.builder()
                .communityId(communityId)
                .testDate(LocalDateTime.now())
                .sourceLocation(req.getSourceLocation())
                .ph(req.getPh())
                .tdsPpm(req.getTdsPpm())
                .hardnessPpm(req.getHardnessPpm())
                .residualChlorinePpm(req.getResidualChlorinePpm())
                .turbidityNtu(req.getTurbidityNtu())
                .status(status)
                .testedBy(req.getTestedBy())
                .labReportUrl(req.getLabReportUrl())
                .build();

        return waterQualityTestRepository.save(test);
    }

    // ── Sump & Tank Cleaning Schedules ─────────────────────────────────────

    public List<TankCleaningSchedule> getCleaningSchedules(Long tankId) {
        return tankCleaningScheduleRepository.findByTankIdOrderByNextDueDateAsc(tankId);
    }

    @Transactional
    public TankCleaningSchedule recordCleaning(Long communityId, TankCleaningRequest req) {
        // Verify tank belongs to community
        getStorageTank(communityId, req.getTankId());

        String status = "COMPLETED";
        if (req.getNextDueDate().isBefore(LocalDate.now())) {
            status = "OVERDUE";
        } else if (req.getLastCleanedDate().isBefore(LocalDate.now().minusDays(180))) {
            status = "SCHEDULED";
        }

        TankCleaningSchedule schedule = TankCleaningSchedule.builder()
                .tankId(req.getTankId())
                .lastCleanedDate(req.getLastCleanedDate())
                .nextDueDate(req.getNextDueDate())
                .cleaningType(req.getCleaningType() != null ? req.getCleaningType() : "MECHANIZED_DEWATERING_UV")
                .vendorName(req.getVendorName())
                .cost(req.getCost() != null ? req.getCost() : BigDecimal.ZERO)
                .status(status)
                .certificateUrl(req.getCertificateUrl())
                .notes(req.getNotes())
                .build();

        return tankCleaningScheduleRepository.save(schedule);
    }

    // ── Overview & Telemetry Summary ───────────────────────────────────────

    public WaterOverviewSummaryDto getOverviewSummary(Long communityId) {
        List<WaterStorageTank> tanks = getStorageTanks(communityId);
        BigDecimal totalCap = tanks.stream()
                .map(WaterStorageTank::getCapacityLiters)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal currentVol = tanks.stream()
                .map(WaterStorageTank::getCurrentLevelLiters)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal storagePct = BigDecimal.ZERO;
        if (totalCap.compareTo(BigDecimal.ZERO) > 0) {
            storagePct = currentVol.multiply(BigDecimal.valueOf(100))
                    .divide(totalCap, 1, RoundingMode.HALF_UP);
        }

        LocalDateTime firstOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<WaterTankerDelivery> thisMonthTankers = waterTankerDeliveryRepository
                .findByCommunityIdAndDeliveryDateBetweenOrderByDeliveryDateDesc(
                        communityId, firstOfMonth, LocalDateTime.now());

        BigDecimal spend = thisMonthTankers.stream()
                .map(WaterTankerDelivery::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<WaterPump> pumps = getPumps(communityId);
        long runningCount = pumps.stream()
                .filter(p -> "RUNNING".equalsIgnoreCase(p.getStatus()))
                .count();

        boolean lowStorageAlert = storagePct.compareTo(BigDecimal.valueOf(25)) < 0;

        return WaterOverviewSummaryDto.builder()
                .totalCapacityLiters(totalCap)
                .currentStorageLiters(currentVol)
                .currentStoragePercentage(storagePct)
                .totalTankersThisMonth((long) thisMonthTankers.size())
                .totalTankerExpenditureThisMonth(spend)
                .totalPumpsCount((long) pumps.size())
                .runningPumpsCount(runningCount)
                .lowStorageAlert(lowStorageAlert)
                .build();
    }

    // ── Seed Helpers ───────────────────────────────────────────────────────

    private List<WaterStorageTank> seedInitialCommunityTanks(Long communityId) {
        log.info("Seeding default water storage sumps and tanks for community {}", communityId);
        List<WaterStorageTank> defaults = List.of(
                WaterStorageTank.builder()
                        .communityId(communityId)
                        .name("Underground Raw Water Sump")
                        .tankType("UG_RAW")
                        .location("Pump House Basement B1")
                        .capacityLiters(BigDecimal.valueOf(150000))
                        .currentLevelLiters(BigDecimal.valueOf(112000))
                        .lowLevelThresholdLiters(BigDecimal.valueOf(35000))
                        .sensorStatus("ONLINE")
                        .lastTelemetryTime(LocalDateTime.now())
                        .build(),
                WaterStorageTank.builder()
                        .communityId(communityId)
                        .name("Domestic Treated Soft Water Sump")
                        .tankType("UG_SOFT")
                        .location("WTP Plant Yard")
                        .capacityLiters(BigDecimal.valueOf(100000))
                        .currentLevelLiters(BigDecimal.valueOf(78500))
                        .lowLevelThresholdLiters(BigDecimal.valueOf(25000))
                        .sensorStatus("ONLINE")
                        .lastTelemetryTime(LocalDateTime.now())
                        .build(),
                WaterStorageTank.builder()
                        .communityId(communityId)
                        .name("Dedicated Fire Safety Reserve Sump")
                        .tankType("UG_FIRE_RESERVE")
                        .location("Fire Pump Station B2")
                        .capacityLiters(BigDecimal.valueOf(200000))
                        .currentLevelLiters(BigDecimal.valueOf(195000))
                        .lowLevelThresholdLiters(BigDecimal.valueOf(180000))
                        .sensorStatus("ONLINE")
                        .lastTelemetryTime(LocalDateTime.now())
                        .build(),
                WaterStorageTank.builder()
                        .communityId(communityId)
                        .name("Tower A Overhead Tank (OHT)")
                        .tankType("OHT_TOWER_A")
                        .location("Tower A Terrace")
                        .capacityLiters(BigDecimal.valueOf(50000))
                        .currentLevelLiters(BigDecimal.valueOf(38200))
                        .lowLevelThresholdLiters(BigDecimal.valueOf(15000))
                        .sensorStatus("ONLINE")
                        .lastTelemetryTime(LocalDateTime.now())
                        .build(),
                WaterStorageTank.builder()
                        .communityId(communityId)
                        .name("Tower B Overhead Tank (OHT)")
                        .tankType("OHT_TOWER_B")
                        .location("Tower B Terrace")
                        .capacityLiters(BigDecimal.valueOf(50000))
                        .currentLevelLiters(BigDecimal.valueOf(34600))
                        .lowLevelThresholdLiters(BigDecimal.valueOf(15000))
                        .sensorStatus("ONLINE")
                        .lastTelemetryTime(LocalDateTime.now())
                        .build()
        );
        return waterStorageTankRepository.saveAll(defaults);
    }

    private List<WaterPump> seedInitialCommunityPumps(Long communityId) {
        log.info("Seeding default water pumps and HYPN motors for community {}", communityId);
        List<WaterPump> defaults = List.of(
                WaterPump.builder()
                        .communityId(communityId)
                        .name("Raw Water Transfer Pump #1")
                        .pumpType("TRANSFER_PUMP")
                        .location("Main Pump House B1")
                        .ratedKw(BigDecimal.valueOf(7.5))
                        .dutyCycleHours(4)
                        .totalRunHours(BigDecimal.valueOf(1240.5))
                        .status("RUNNING")
                        .isDutyPump(true)
                        .motorHealthRating("EXCELLENT")
                        .lastServiceDate(LocalDate.now().minusDays(20))
                        .build(),
                WaterPump.builder()
                        .communityId(communityId)
                        .name("Raw Water Transfer Pump #2 (Standby)")
                        .pumpType("TRANSFER_PUMP")
                        .location("Main Pump House B1")
                        .ratedKw(BigDecimal.valueOf(7.5))
                        .dutyCycleHours(4)
                        .totalRunHours(BigDecimal.valueOf(1185.0))
                        .status("STANDBY")
                        .isDutyPump(false)
                        .motorHealthRating("GOOD")
                        .lastServiceDate(LocalDate.now().minusDays(20))
                        .build(),
                WaterPump.builder()
                        .communityId(communityId)
                        .name("Hydro-Pneumatic (HYPN) Booster #1")
                        .pumpType("BOOSTER_PUMP")
                        .location("Clubhouse Utility Room")
                        .ratedKw(BigDecimal.valueOf(11.0))
                        .dutyCycleHours(6)
                        .totalRunHours(BigDecimal.valueOf(2450.0))
                        .status("RUNNING")
                        .isDutyPump(true)
                        .motorHealthRating("GOOD")
                        .lastServiceDate(LocalDate.now().minusDays(45))
                        .build(),
                WaterPump.builder()
                        .communityId(communityId)
                        .name("Borewell Submersible Pump #1")
                        .pumpType("BOREWELL_PUMP")
                        .location("East Gate Borewell Point")
                        .ratedKw(BigDecimal.valueOf(5.5))
                        .dutyCycleHours(3)
                        .totalRunHours(BigDecimal.valueOf(890.0))
                        .status("STANDBY")
                        .isDutyPump(false)
                        .motorHealthRating("GOOD")
                        .lastServiceDate(LocalDate.now().minusDays(60))
                        .build(),
                WaterPump.builder()
                        .communityId(communityId)
                        .name("Fire Hydrant Jockey Pump")
                        .pumpType("FIRE_HYDRANT")
                        .location("Fire Safety Room B2")
                        .ratedKw(BigDecimal.valueOf(15.0))
                        .dutyCycleHours(24)
                        .totalRunHours(BigDecimal.valueOf(120.0))
                        .status("STANDBY")
                        .isDutyPump(false)
                        .motorHealthRating("EXCELLENT")
                        .lastServiceDate(LocalDate.now().minusDays(15))
                        .build()
        );
        return waterPumpRepository.saveAll(defaults);
    }
}
