package com.manacommunity.api.service;

import com.manacommunity.api.dto.GeneratorDtos.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("generatorMaintenanceService")
@RequiredArgsConstructor
public class GeneratorMaintenanceService {

    private final GeneratorAssetRepository generatorAssetRepository;
    private final GeneratorRunLogRepository generatorRunLogRepository;
    private final GeneratorServiceRecordRepository generatorServiceRecordRepository;
    private final GeneratorFuelRefillRepository generatorFuelRefillRepository;
    private final GeneratorAmcContractRepository generatorAmcContractRepository;

    @Transactional
    public List<GeneratorAsset> getGenerators(Long communityId) {
        List<GeneratorAsset> list = generatorAssetRepository.findByCommunityIdOrderByNameAsc(communityId);
        if (list.isEmpty()) {
            list = seedInitialCommunityGenerators(communityId);
        }
        return list;
    }

    public GeneratorAsset getGenerator(Long communityId, Long id) {
        return generatorAssetRepository.findByCommunityIdAndId(communityId, id)
                .orElseThrow(() -> new IllegalArgumentException("Generator not found with id: " + id));
    }

    @Transactional
    public GeneratorRunLog logRun(Long communityId, GeneratorRunRequest req) {
        GeneratorAsset gen = getGenerator(communityId, req.getGeneratorId());

        long seconds = Duration.between(req.getStartTime(), req.getEndTime()).getSeconds();
        BigDecimal durationHours = BigDecimal.valueOf(Math.max(seconds, 360))
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);

        BigDecimal dieselConsumed = req.getStartFuelLiters().subtract(req.getEndFuelLiters());
        if (dieselConsumed.compareTo(BigDecimal.ZERO) < 0) {
            dieselConsumed = BigDecimal.ZERO;
        }

        BigDecimal efficiency = BigDecimal.ZERO;
        if (dieselConsumed.compareTo(BigDecimal.ZERO) > 0 && req.getUnitsGeneratedKwh() != null) {
            efficiency = req.getUnitsGeneratedKwh().divide(dieselConsumed, 2, RoundingMode.HALF_UP);
        }

        GeneratorRunLog runLog = GeneratorRunLog.builder()
                .generator(gen)
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .durationHours(durationHours)
                .startFuelLiters(req.getStartFuelLiters())
                .endFuelLiters(req.getEndFuelLiters())
                .dieselConsumedLiters(dieselConsumed)
                .unitsGeneratedKwh(req.getUnitsGeneratedKwh())
                .fuelEfficiencyKwhPerL(efficiency)
                .reason(req.getReason() != null ? req.getReason() : "GRID_OUTAGE")
                .operatorName(req.getOperatorName())
                .notes(req.getNotes())
                .build();

        GeneratorRunLog saved = generatorRunLogRepository.save(runLog);

        // Update generator total run hours and fuel level
        gen.setTotalRunHours(gen.getTotalRunHours().add(durationHours));
        gen.setCurrentFuelLiters(req.getEndFuelLiters());
        gen.setStatus("STANDBY");
        generatorAssetRepository.save(gen);

        log.info("Logged generator run: genId={}, duration={}h, consumed={}L", gen.getId(), durationHours, dieselConsumed);
        return saved;
    }

    @Transactional
    public GeneratorFuelRefill logRefill(Long communityId, GeneratorRefillRequest req) {
        GeneratorAsset gen = getGenerator(communityId, req.getGeneratorId());

        BigDecimal totalCost = req.getQuantityLiters().multiply(req.getPricePerLiter()).setScale(2, RoundingMode.HALF_UP);

        GeneratorFuelRefill refill = GeneratorFuelRefill.builder()
                .generator(gen)
                .refillDate(req.getRefillDate())
                .quantityLiters(req.getQuantityLiters())
                .pricePerLiter(req.getPricePerLiter())
                .totalCost(totalCost)
                .supplierName(req.getSupplierName())
                .billNumber(req.getBillNumber())
                .enteredBy(req.getEnteredBy())
                .build();

        GeneratorFuelRefill saved = generatorFuelRefillRepository.save(refill);

        // Increment current fuel up to tank capacity
        BigDecimal newFuel = gen.getCurrentFuelLiters().add(req.getQuantityLiters());
        if (newFuel.compareTo(gen.getFuelTankCapacityLiters()) > 0) {
            newFuel = gen.getFuelTankCapacityLiters();
        }
        gen.setCurrentFuelLiters(newFuel);
        generatorAssetRepository.save(gen);

        log.info("Logged fuel refill: genId={}, liters={}, newLevel={}", gen.getId(), req.getQuantityLiters(), newFuel);
        return saved;
    }

    @Transactional
    public GeneratorServiceRecord recordService(Long communityId, GeneratorServiceRecordRequest req) {
        GeneratorAsset gen = getGenerator(communityId, req.getGeneratorId());

        GeneratorServiceRecord record = GeneratorServiceRecord.builder()
                .generator(gen)
                .serviceType(req.getServiceType())
                .serviceDate(req.getServiceDate())
                .runHoursAtService(req.getRunHoursAtService())
                .vendorName(req.getVendorName())
                .technicianName(req.getTechnicianName())
                .cost(req.getCost() != null ? req.getCost() : BigDecimal.ZERO)
                .partsReplaced(req.getPartsReplaced())
                .notes(req.getNotes())
                .status(req.getStatus() != null ? req.getStatus() : "COMPLETED")
                .ticketId(req.getTicketId())
                .build();

        GeneratorServiceRecord saved = generatorServiceRecordRepository.save(record);

        // If completed B-Check (250h) or major C-Check, reset lastServiceHours
        if ("COMPLETED".equalsIgnoreCase(record.getStatus()) &&
                ("B_CHECK_250H".equalsIgnoreCase(record.getServiceType()) || "C_CHECK_MAJOR".equalsIgnoreCase(record.getServiceType()))) {
            gen.setLastServiceHours(req.getRunHoursAtService());
            generatorAssetRepository.save(gen);
            log.info("Reset 250h service countdown for genId={} to {} hours", gen.getId(), req.getRunHoursAtService());
        }

        return saved;
    }

    public List<GeneratorRunLog> getRunLogs(Long communityId, Long generatorId) {
        if (generatorId != null) {
            getGenerator(communityId, generatorId); // ensures community scope
            return generatorRunLogRepository.findByGeneratorIdOrderByStartTimeDesc(generatorId);
        }
        return generatorRunLogRepository.findByGeneratorCommunityIdOrderByStartTimeDesc(communityId);
    }

    public List<GeneratorServiceRecord> getServiceRecords(Long communityId, Long generatorId) {
        if (generatorId != null) {
            getGenerator(communityId, generatorId);
            return generatorServiceRecordRepository.findByGeneratorIdOrderByServiceDateDesc(generatorId);
        }
        return generatorServiceRecordRepository.findByGeneratorCommunityIdOrderByServiceDateDesc(communityId);
    }

    public List<GeneratorFuelRefill> getFuelRefills(Long communityId, Long generatorId) {
        if (generatorId != null) {
            getGenerator(communityId, generatorId);
            return generatorFuelRefillRepository.findByGeneratorIdOrderByRefillDateDesc(generatorId);
        }
        return generatorFuelRefillRepository.findByGeneratorCommunityIdOrderByRefillDateDesc(communityId);
    }

    public List<GeneratorAmcContract> getAmcContracts(Long communityId, Long generatorId) {
        if (generatorId != null) {
            getGenerator(communityId, generatorId);
            return generatorAmcContractRepository.findByGeneratorIdOrderByStartDateDesc(generatorId);
        }
        return generatorAmcContractRepository.findByGeneratorCommunityIdOrderByStartDateDesc(communityId);
    }

    public List<ServiceAlertDto> checkServiceAlerts(Long communityId, Long generatorId) {
        List<GeneratorAsset> targetGens = generatorId != null
                ? List.of(getGenerator(communityId, generatorId))
                : getGenerators(communityId);

        List<ServiceAlertDto> alerts = new ArrayList<>();
        for (GeneratorAsset gen : targetGens) {
            BigDecimal hoursSinceLastService = gen.getTotalRunHours().subtract(gen.getLastServiceHours()).setScale(1, RoundingMode.HALF_UP);
            BigDecimal interval = BigDecimal.valueOf(gen.getServiceIntervalHours());
            BigDecimal hoursRemainingOrOverdue = interval.subtract(hoursSinceLastService).setScale(1, RoundingMode.HALF_UP);
            boolean isOverdue = hoursSinceLastService.compareTo(interval) >= 0;

            String alertSeverity = "NORMAL";
            String recommended = "Standard Routine A-Check & Periodic Inspection";

            if (isOverdue) {
                alertSeverity = "OVERDUE";
                recommended = "Urgent 250h B-Check Service Required (Oil & Filter Replacement)";
            } else if (hoursRemainingOrOverdue.compareTo(BigDecimal.valueOf(30)) <= 0) {
                alertSeverity = "UPCOMING";
                recommended = "Upcoming 250h B-Check Service (within 30 operating hours)";
            }

            alerts.add(ServiceAlertDto.builder()
                    .generatorId(gen.getId())
                    .generatorName(gen.getName())
                    .hoursSinceLastService(hoursSinceLastService)
                    .intervalHours(gen.getServiceIntervalHours())
                    .hoursRemainingOrOverdue(hoursRemainingOrOverdue)
                    .isOverdue(isOverdue)
                    .alertSeverity(alertSeverity)
                    .recommendedService(recommended)
                    .build());
        }
        return alerts;
    }

    private List<GeneratorAsset> seedInitialCommunityGenerators(Long communityId) {
        log.info("Seeding standard default generators for communityId={}", communityId);
        GeneratorAsset gen1 = GeneratorAsset.builder()
                .communityId(communityId)
                .name("Substation Main DG 1")
                .makeModel("Cummins QSL9-G5 (380 kVA)")
                .serialNumber("CUM-" + communityId + "-8841")
                .capacityKva(380)
                .location("Basement 2, Substation Room A")
                .fuelTankCapacityLiters(BigDecimal.valueOf(800))
                .currentFuelLiters(BigDecimal.valueOf(640))
                .totalRunHours(BigDecimal.valueOf(1248.5))
                .lastServiceHours(BigDecimal.valueOf(1020.0))
                .serviceIntervalHours(250)
                .batteryVoltage(BigDecimal.valueOf(12.8))
                .coolantTempCelsius(65)
                .oilPressureBar(BigDecimal.valueOf(4.2))
                .status("STANDBY")
                .amcVendor("Cummins India Authorized - Powertech Services")
                .amcExpiryDate(LocalDate.now().plusMonths(10))
                .meterSerialNumber("DG-BACKUP-01")
                .build();

        GeneratorAsset gen2 = GeneratorAsset.builder()
                .communityId(communityId)
                .name("Clubhouse & Lift DG 2")
                .makeModel("Kirloskar Green KG125WS (160 kVA)")
                .serialNumber("KIR-" + communityId + "-1192")
                .capacityKva(160)
                .location("Clubhouse Rear Utility Yard")
                .fuelTankCapacityLiters(BigDecimal.valueOf(350))
                .currentFuelLiters(BigDecimal.valueOf(290))
                .totalRunHours(BigDecimal.valueOf(642.0))
                .lastServiceHours(BigDecimal.valueOf(385.0))
                .serviceIntervalHours(250)
                .batteryVoltage(BigDecimal.valueOf(12.4))
                .coolantTempCelsius(70)
                .oilPressureBar(BigDecimal.valueOf(3.8))
                .status("STANDBY")
                .amcVendor("Kirloskar Care South Services")
                .amcExpiryDate(LocalDate.now().plusMonths(6))
                .meterSerialNumber("DG-CLUB-02")
                .build();

        gen1 = generatorAssetRepository.save(gen1);
        gen2 = generatorAssetRepository.save(gen2);

        // Seed initial AMC contracts
        generatorAmcContractRepository.save(GeneratorAmcContract.builder()
                .generator(gen1)
                .vendorName("Cummins India Authorized - Powertech Services")
                .contractNumber("CUM-AMC-" + communityId + "-778")
                .startDate(LocalDate.now().minusMonths(2))
                .endDate(LocalDate.now().plusMonths(10))
                .annualCost(BigDecimal.valueOf(95000))
                .visitsIncluded(4)
                .visitsCompleted(2)
                .emergencyHelpline("+91 80 4123 9900 / 1800-210-2525")
                .slaHours(2)
                .status("ACTIVE")
                .build());

        generatorAmcContractRepository.save(GeneratorAmcContract.builder()
                .generator(gen2)
                .vendorName("Kirloskar Care South Services")
                .contractNumber("KOEL-AMC-" + communityId + "-412")
                .startDate(LocalDate.now().minusMonths(6))
                .endDate(LocalDate.now().plusMonths(6))
                .annualCost(BigDecimal.valueOf(52000))
                .visitsIncluded(4)
                .visitsCompleted(3)
                .emergencyHelpline("+91 80 2839 4400 / 1800-233-3344")
                .slaHours(4)
                .status("ACTIVE")
                .build());

        return List.of(gen1, gen2);
    }
}
