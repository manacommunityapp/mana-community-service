package com.manacommunity.api.procurement.service;

import com.manacommunity.api.procurement.entity.ProcAmcContract;
import com.manacommunity.api.procurement.entity.ProcMaintenanceSchedule;
import com.manacommunity.api.procurement.repository.ProcAmcContractRepository;
import com.manacommunity.api.procurement.repository.ProcMaintenanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmcSchedulerService {

    private final ProcAmcContractRepository contractRepo;
    private final ProcMaintenanceScheduleRepository scheduleRepo;

    @Transactional
    public GenerateResult generateSchedules(Long communityId, int monthsAhead) {
        List<ProcAmcContract> activeContracts = contractRepo.findByCommunityIdAndStatus(communityId, ProcAmcContract.AmcStatus.ACTIVE);
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusMonths(monthsAhead);
        int created = 0;

        for (ProcAmcContract contract : activeContracts) {
            List<ProcMaintenanceSchedule> existing = scheduleRepo.findByCommunityIdAndAssetIdentifier(
                    communityId, contract.getAssetIdentifier());
            Set<LocalDate> existingDates = existing.stream()
                    .map(ProcMaintenanceSchedule::getScheduledDate).collect(Collectors.toSet());

            LocalDate nextDate = findNextServiceDate(contract, today);
            while (nextDate != null && !nextDate.isAfter(horizon) && !nextDate.isAfter(contract.getEndDate())) {
                if (!existingDates.contains(nextDate)) {
                    ProcMaintenanceSchedule schedule = new ProcMaintenanceSchedule();
                    schedule.setCommunityId(communityId);
                    schedule.setAmcContract(contract);
                    schedule.setAssetIdentifier(contract.getAssetIdentifier());
                    schedule.setScheduledDate(nextDate);
                    schedule.setStatus(ProcMaintenanceSchedule.ScheduleStatus.UPCOMING);
                    schedule.setChecklistJson(generateChecklist(contract.getAssetCategory()));
                    scheduleRepo.save(schedule);
                    created++;
                }
                nextDate = nextDate.plusDays(contract.getServiceIntervalDays());
            }
        }

        log.info("Generated {} maintenance schedules for community {} ({} months ahead)", created, communityId, monthsAhead);
        return new GenerateResult(created, activeContracts.size(), monthsAhead);
    }

    public List<ExpiryAlert> checkAmcExpiries(Long communityId) {
        LocalDate alertDate = LocalDate.now().plusDays(30);
        List<ProcAmcContract> expiring = contractRepo.findExpiringContracts(communityId, alertDate);
        List<ProcAmcContract> warrantyExpiring = contractRepo.findExpiringWarranties(communityId, alertDate);

        List<ExpiryAlert> alerts = new ArrayList<>();
        for (ProcAmcContract c : expiring) {
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), c.getEndDate());
            alerts.add(new ExpiryAlert(c.getContractNumber(), c.getAssetIdentifier(), c.getVendorName(),
                    c.getAssetCategory(), "AMC_EXPIRY", c.getEndDate(), daysLeft, c.getAnnualValue()));
            if (c.getStatus() != ProcAmcContract.AmcStatus.EXPIRING_SOON) {
                c.setStatus(ProcAmcContract.AmcStatus.EXPIRING_SOON);
                contractRepo.save(c);
            }
        }
        for (ProcAmcContract c : warrantyExpiring) {
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), c.getWarrantyExpiry());
            alerts.add(new ExpiryAlert(c.getContractNumber(), c.getAssetIdentifier(), c.getVendorName(),
                    c.getAssetCategory(), "WARRANTY_EXPIRY", c.getWarrantyExpiry(), daysLeft, c.getAnnualValue()));
        }

        alerts.sort(Comparator.comparingLong(ExpiryAlert::daysLeft));
        return alerts;
    }

    public List<ProcMaintenanceSchedule> getOverdueSchedules(Long communityId) {
        return scheduleRepo.findOverdueSchedules(communityId, LocalDate.now());
    }

    @Transactional
    public ProcMaintenanceSchedule completeSchedule(Long scheduleId, String technicianName, String notes, int spareParts, String spareDetails) {
        ProcMaintenanceSchedule schedule = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
        schedule.setStatus(ProcMaintenanceSchedule.ScheduleStatus.COMPLETED);
        schedule.setTechnicianName(technicianName);
        schedule.setCompletionNotes(notes);
        schedule.setCompletedAt(LocalDateTime.now());
        schedule.setSparePartsUsed(spareParts);
        schedule.setSparePartDetails(spareDetails);

        if (spareParts > 0) {
            ProcAmcContract contract = schedule.getAmcContract();
            if (contract.getSparePartThreshold() > 0) {
                List<ProcMaintenanceSchedule> recentCompleted = scheduleRepo
                        .findByAmcContractIdAndStatus(contract.getId(), ProcMaintenanceSchedule.ScheduleStatus.COMPLETED);
                int totalSpares = recentCompleted.stream().mapToInt(ProcMaintenanceSchedule::getSparePartsUsed).sum() + spareParts;
                if (totalSpares > contract.getSparePartThreshold()) {
                    log.warn("SPARE PART ALERT: {} ({}) consumed {} parts (threshold={})",
                            contract.getAssetIdentifier(), contract.getAssetCategory(),
                            totalSpares, contract.getSparePartThreshold());
                }
            }
        }
        return scheduleRepo.save(schedule);
    }

    public List<ProcMaintenanceSchedule> getUpcoming(Long communityId, int days) {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(days);
        return scheduleRepo.findByCommunityIdAndScheduledDateBetween(communityId, start, end);
    }

    public Map<String, Object> getDashboardSummary(Long communityId) {
        List<ProcAmcContract> contracts = contractRepo.findByCommunityId(communityId);
        long active = contracts.stream().filter(c -> c.getStatus() == ProcAmcContract.AmcStatus.ACTIVE).count();
        long expiringSoon = contracts.stream().filter(c -> c.getStatus() == ProcAmcContract.AmcStatus.EXPIRING_SOON).count();
        long expired = contracts.stream().filter(c -> c.getStatus() == ProcAmcContract.AmcStatus.EXPIRED).count();

        List<ProcMaintenanceSchedule> overdue = getOverdueSchedules(communityId);
        List<ProcMaintenanceSchedule> upcoming = getUpcoming(communityId, 7);

        Map<String, Long> byCategory = contracts.stream()
                .collect(Collectors.groupingBy(ProcAmcContract::getAssetCategory, Collectors.counting()));

        return Map.of(
                "totalContracts", contracts.size(), "active", active, "expiringSoon", expiringSoon,
                "expired", expired, "overdueSchedules", overdue.size(), "upcomingThisWeek", upcoming.size(),
                "byCategory", byCategory);
    }

    private LocalDate findNextServiceDate(ProcAmcContract contract, LocalDate fromDate) {
        LocalDate next = contract.getStartDate();
        while (next.isBefore(fromDate)) {
            next = next.plusDays(contract.getServiceIntervalDays());
        }
        return next.isAfter(contract.getEndDate()) ? null : next;
    }

    private String generateChecklist(String category) {
        return switch (category) {
            case "ELEVATOR" -> "[{\"item\":\"Check door alignment\",\"done\":false},{\"item\":\"Lubricate guide rails\",\"done\":false},{\"item\":\"Test emergency brakes\",\"done\":false},{\"item\":\"Inspect wire ropes\",\"done\":false},{\"item\":\"Check control panel\",\"done\":false},{\"item\":\"Test intercom/alarm\",\"done\":false}]";
            case "DG_SET" -> "[{\"item\":\"Check fuel level & quality\",\"done\":false},{\"item\":\"Inspect coolant level\",\"done\":false},{\"item\":\"Test auto-transfer switch\",\"done\":false},{\"item\":\"Check battery condition\",\"done\":false},{\"item\":\"Run load test\",\"done\":false},{\"item\":\"Check exhaust system\",\"done\":false}]";
            case "STP" -> "[{\"item\":\"Check inlet pH level\",\"done\":false},{\"item\":\"Inspect aeration blowers\",\"done\":false},{\"item\":\"Test BOD/COD levels\",\"done\":false},{\"item\":\"Clean filters\",\"done\":false},{\"item\":\"Check sludge level\",\"done\":false},{\"item\":\"Verify chlorine dosing\",\"done\":false}]";
            case "FIRE_SYSTEM" -> "[{\"item\":\"Test fire alarm panel\",\"done\":false},{\"item\":\"Check sprinkler pressure\",\"done\":false},{\"item\":\"Inspect extinguishers\",\"done\":false},{\"item\":\"Test hydrant pump\",\"done\":false},{\"item\":\"Check emergency lighting\",\"done\":false}]";
            default -> "[{\"item\":\"General inspection\",\"done\":false},{\"item\":\"Performance test\",\"done\":false},{\"item\":\"Clean components\",\"done\":false},{\"item\":\"Check safety features\",\"done\":false}]";
        };
    }

    public record GenerateResult(int schedulesCreated, int contractsProcessed, int monthsAhead) {}
    public record ExpiryAlert(String contractNumber, String assetIdentifier, String vendorName,
                              String category, String alertType, LocalDate expiryDate, long daysLeft,
                              java.math.BigDecimal annualValue) {}
}
