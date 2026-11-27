package com.manacommunity.api.dailyhelp.service.impl;

import com.manacommunity.api.dailyhelp.dto.DailyHelpDtos.*;
import com.manacommunity.api.dailyhelp.entity.*;
import com.manacommunity.api.dailyhelp.repository.DailyHelpRepositories.*;
import com.manacommunity.api.dailyhelp.service.DailyHelpAttendanceService;
import com.manacommunity.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyHelpAttendanceServiceImpl implements DailyHelpAttendanceService {

    private final GlobalStaffProfileRepository staffProfileRepo;
    private final SocietyStaffAffiliationRepository affiliationRepo;
    private final FlatStaffContractRepository contractRepo;
    private final FlatAttendanceSessionRepository attendanceRepo;
    private final StaffMonthlySalaryLedgerRepository salaryRepo;

    @Override
    @Transactional
    public List<FlatAttendanceReconciliationDto> reconcileGateDwellSession(GateDwellIntervalDto dwell) {
        log.info("Reconciling gate dwell for staff {} in community {} on {}",
                dwell.getStaffUuid(), dwell.getCommunityId(), dwell.getWorkDate());

        List<FlatStaffContract> contracts = contractRepo
                .findByCommunityIdAndAffiliation_StaffProfile_StaffUuidAndIsActiveTrue(
                        dwell.getCommunityId(), dwell.getStaffUuid());

        List<FlatAttendanceReconciliationDto> results = new ArrayList<>();
        int dayOfWeekBit = 1 << (dwell.getWorkDate().getDayOfWeek().getValue() - 1); // 1=Mon, 2=Tue, ...

        for (FlatStaffContract contract : contracts) {
            // Check day of week schedule
            if ((contract.getDaysOfWeekMask() & dayOfWeekBit) == 0) {
                continue; // Not contracted for this day of week
            }

            int scheduledMinutes = (int) Duration.between(
                    contract.getScheduledStartTime(), contract.getScheduledEndTime()).toMinutes();
            if (scheduledMinutes <= 0) scheduledMinutes = 60;

            // Calculate overlap between gate dwell and scheduled slot
            LocalTime overlapStart = dwell.getGateEntryTime().isAfter(contract.getScheduledStartTime())
                    ? dwell.getGateEntryTime() : contract.getScheduledStartTime();
            LocalTime overlapEnd = dwell.getGateExitTime().isBefore(contract.getScheduledEndTime())
                    ? dwell.getGateExitTime() : contract.getScheduledEndTime();

            int actualOverlapMinutes = 0;
            if (overlapStart.isBefore(overlapEnd)) {
                actualOverlapMinutes = (int) Duration.between(overlapStart, overlapEnd).toMinutes();
            }

            String status;
            BigDecimal multiplier;

            if (actualOverlapMinutes <= 0) {
                status = "GATE_ABSENT";
                multiplier = BigDecimal.ZERO;
            } else {
                double ratio = (double) actualOverlapMinutes / scheduledMinutes;
                if (ratio >= 0.70) {
                    status = "PRESENT";
                    multiplier = new BigDecimal("1.00");
                } else if (ratio >= 0.30) {
                    status = "HALF_DAY";
                    multiplier = new BigDecimal("0.50");
                } else {
                    status = "ABSENT";
                    multiplier = BigDecimal.ZERO;
                }
            }

            // Upsert attendance session
            FlatAttendanceSession session = attendanceRepo
                    .findByContract_IdAndWorkDate(contract.getId(), dwell.getWorkDate())
                    .orElseGet(() -> FlatAttendanceSession.builder()
                            .contract(contract)
                            .workDate(dwell.getWorkDate())
                            .build());

            session.setScheduledMinutes(scheduledMinutes);
            session.setSocietyDwellMinutes(actualOverlapMinutes);
            session.setAttendanceStatus(status);
            session.setProRataMultiplier(multiplier);
            session.setReconciledAt(LocalDateTime.now());
            attendanceRepo.save(session);

            results.add(FlatAttendanceReconciliationDto.builder()
                    .contractId(contract.getId())
                    .flatId(contract.getFlatId())
                    .staffName(contract.getAffiliation().getStaffProfile().getDisplayName())
                    .serviceType(contract.getServiceType())
                    .workDate(dwell.getWorkDate())
                    .status(status)
                    .scheduledMinutes(scheduledMinutes)
                    .actualDwellMinutes(actualOverlapMinutes)
                    .proRataMultiplier(multiplier)
                    .noticeMessage(String.format("Attendance %s recorded based on %d/%d min society dwell",
                            status, actualOverlapMinutes, scheduledMinutes))
                    .build());
        }

        return results;
    }

    @Override
    @Transactional
    public MonthlySalarySettlementDto calculateMonthlySalarySettlement(Long contractId, String billingMonth) {
        FlatStaffContract contract = contractRepo.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        YearMonth ym = YearMonth.parse(billingMonth);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        int calendarDays = ym.lengthOfMonth();

        List<FlatAttendanceSession> sessions = attendanceRepo
                .findByContract_IdAndWorkDateBetween(contractId, startDate, endDate);

        int scheduledWorkingDays = 0;
        int presentDays = 0;
        int paidLeaveDays = 0;
        int unauthorizedAbsenceDays = 0;

        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            int dayBit = 1 << (d.getDayOfWeek().getValue() - 1);
            if ((contract.getDaysOfWeekMask() & dayBit) != 0) {
                scheduledWorkingDays++;
            }
        }

        for (FlatAttendanceSession s : sessions) {
            if ("PRESENT".equalsIgnoreCase(s.getAttendanceStatus())) {
                presentDays++;
            } else if ("LEAVE_PAID".equalsIgnoreCase(s.getAttendanceStatus())) {
                paidLeaveDays++;
            } else if ("HALF_DAY".equalsIgnoreCase(s.getAttendanceStatus())) {
                presentDays++; // Counted as half credit in pro-rata calculation
            }
        }

        // Apply paid leave allowance
        int allowedLeaves = contract.getLeaveAllowancePerMonth();
        int excessLeaves = Math.max(0, (scheduledWorkingDays - presentDays) - allowedLeaves);
        unauthorizedAbsenceDays = excessLeaves;

        BigDecimal baseStipend = contract.getMonthlyStipend();
        BigDecimal dailyRate = scheduledWorkingDays > 0
                ? baseStipend.divide(BigDecimal.valueOf(scheduledWorkingDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal proRataDeduction = dailyRate.multiply(BigDecimal.valueOf(unauthorizedAbsenceDays))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal netPayable = baseStipend.subtract(proRataDeduction).max(BigDecimal.ZERO);

        StaffMonthlySalaryLedger ledger = salaryRepo
                .findByContract_IdAndBillingMonth(contractId, billingMonth)
                .orElseGet(() -> StaffMonthlySalaryLedger.builder()
                        .contract(contract)
                        .billingMonth(billingMonth)
                        .build());

        ledger.setCalendarDays(calendarDays);
        ledger.setScheduledWorkingDays(scheduledWorkingDays);
        ledger.setPresentDays(presentDays);
        ledger.setPaidLeaveDays(Math.min(allowedLeaves, scheduledWorkingDays - presentDays));
        ledger.setUnpaidAbsenceDays(unauthorizedAbsenceDays);
        ledger.setBaseStipend(baseStipend);
        ledger.setProRataDeduction(proRataDeduction);
        ledger.setNetPayable(netPayable);
        ledger.setCalculatedAt(LocalDateTime.now());
        salaryRepo.save(ledger);

        return MonthlySalarySettlementDto.builder()
                .contractId(contractId)
                .flatId(contract.getFlatId())
                .billingMonth(billingMonth)
                .staffMaskedName(contract.getAffiliation().getStaffProfile().getDisplayName())
                .serviceType(contract.getServiceType())
                .calendarDays(calendarDays)
                .scheduledWorkingDays(scheduledWorkingDays)
                .presentDays(presentDays)
                .paidLeaveDays(ledger.getPaidLeaveDays())
                .unpaidAbsenceDays(unauthorizedAbsenceDays)
                .baseStipend(baseStipend)
                .proRataDeduction(proRataDeduction)
                .netPayable(netPayable)
                .paymentStatus(ledger.getPaymentStatus())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlatAttendanceReconciliationDto> getFlatAttendanceHistory(
            Long communityId, Long flatId, LocalDate startDate, LocalDate endDate) {
        // Enforces strict tenant isolation: queries strictly for (communityId, flatId)
        List<FlatAttendanceSession> sessions = attendanceRepo
                .findFlatAttendanceHistory(communityId, flatId, startDate, endDate);

        return sessions.stream().map(s -> FlatAttendanceReconciliationDto.builder()
                .contractId(s.getContract().getId())
                .flatId(s.getContract().getFlatId())
                .staffName(s.getContract().getAffiliation().getStaffProfile().getDisplayName())
                .serviceType(s.getContract().getServiceType())
                .workDate(s.getWorkDate())
                .status(s.getAttendanceStatus())
                .scheduledMinutes(s.getScheduledMinutes())
                .actualDwellMinutes(s.getSocietyDwellMinutes())
                .proRataMultiplier(s.getProRataMultiplier())
                .noticeMessage("Tenant-isolated flat attendance")
                .build()).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CrossSocietyStaffProfileDto getPublicCrossSocietyProfile(String staffUuid) {
        GlobalStaffProfile staff = staffProfileRepo.findByStaffUuid(staffUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found: " + staffUuid));

        List<SocietyStaffAffiliation> affiliations = affiliationRepo.findByStaffProfile_StaffUuid(staffUuid);
        List<String> communityNames = affiliations.stream()
                .map(a -> a.getCommunity().getName())
                .toList();

        return CrossSocietyStaffProfileDto.builder()
                .staffUuid(staff.getStaffUuid())
                .displayName(staff.getDisplayName())
                .primaryCategory(staff.getPrimaryCategory())
                .maskedPhone(staff.getMaskedPhone())
                .aggregateRating(staff.getAggregateRating())
                .totalReviews(staff.getRatingSampleSize())
                .affiliatedCommunities(communityNames)
                .build();
    }
}
