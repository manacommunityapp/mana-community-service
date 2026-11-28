package com.manacommunity.api.dailyhelp.service.impl;

import com.manacommunity.api.dailyhelp.dto.DailyHelpDtos.*;
import com.manacommunity.api.dailyhelp.entity.*;
import com.manacommunity.api.dailyhelp.repository.DailyHelpRepositories.*;
import com.manacommunity.api.dailyhelp.service.DailyHelpAttendanceService;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.NotificationCategory;
import com.manacommunity.api.model.NotificationPriority;
import com.manacommunity.api.model.NotificationType;
import com.manacommunity.api.model.ReferenceType;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.service.NotificationManagementService;
import com.manacommunity.api.staff.service.StaffAccessRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyHelpAttendanceServiceImpl implements DailyHelpAttendanceService {

    private final GlobalStaffProfileRepository staffProfileRepo;
    private final SocietyStaffAffiliationRepository affiliationRepo;
    private final FlatStaffContractRepository contractRepo;
    private final FlatAttendanceSessionRepository attendanceRepo;
    private final StaffMonthlySalaryLedgerRepository salaryRepo;

    @Autowired(required = false)
    private NotificationManagementService notificationService;

    @Autowired(required = false)
    private StaffAccessRuleService accessRuleService;

    @Autowired(required = false)
    private CommunityRepository communityRepo;

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

    @Override
    @Transactional
    public StaffGateScanResultDto recordGateEntry(StaffGateScanRequest request) {
        if (request.getCommunityId() == null || request.getStaffUuid() == null || request.getStaffUuid().isBlank()) {
            throw new InvalidInputException("communityId and staffUuid are required");
        }
        LocalDateTime eventTs = request.getEventTimestamp() != null ? request.getEventTimestamp() : LocalDateTime.now();
        String gateName = request.getGateName() != null && !request.getGateName().isBlank() ? request.getGateName() : "Main Gate";

        SocietyStaffAffiliation affiliation = affiliationRepo
                .findByCommunityIdAndStaffProfile_StaffUuid(request.getCommunityId(), request.getStaffUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Staff affiliation not found: " + request.getStaffUuid()));

        if (!"ACTIVE".equalsIgnoreCase(affiliation.getStatus())) {
            throw new UnauthorizedActionException("Staff affiliation is not ACTIVE: " + affiliation.getStatus());
        }
        if ("IN".equalsIgnoreCase(affiliation.getCurrentGateState())) {
            throw new InvalidInputException("Staff is already checked IN at society gate");
        }
        if (accessRuleService != null) {
            String day = eventTs.getDayOfWeek().name();
            if (!accessRuleService.isAccessAllowed(affiliation.getId(), day, eventTs.toLocalTime())) {
                throw new UnauthorizedActionException("Gate entry outside allowed staff curfew hours");
            }
        }

        affiliation.setCurrentGateState("IN");
        affiliation.setLastGateEventAt(eventTs);
        affiliationRepo.save(affiliation);

        GlobalStaffProfile profile = affiliation.getStaffProfile();
        List<FlatStaffContract> contracts = contractRepo
                .findByCommunityIdAndAffiliation_StaffProfile_StaffUuidAndIsActiveTrue(
                        request.getCommunityId(), request.getStaffUuid());

        int dayOfWeekBit = 1 << (eventTs.getDayOfWeek().getValue() - 1);
        Set<Long> notifiedUsers = new LinkedHashSet<>();
        List<Long> notifiedFlatIds = new ArrayList<>();
        String timeStr = eventTs.format(DateTimeFormatter.ofPattern("hh:mm a"));

        for (FlatStaffContract contract : contracts) {
            if ((contract.getDaysOfWeekMask() & dayOfWeekBit) == 0) {
                continue;
            }
            notifiedFlatIds.add(contract.getFlatId());
            if (contract.getResident() != null && contract.getResident().getId() != null
                    && notifiedUsers.add(contract.getResident().getId()) && notificationService != null) {
                try {
                    notificationService.createNotification(
                            contract.getResident().getId(),
                            NotificationType.VISITOR_CHECK_IN,
                            NotificationCategory.COMMUNITY,
                            "Daily Help Arrived",
                            String.format("%s (%s, Badge %s) entered %s at %s.",
                                    profile.getDisplayName(),
                                    contract.getServiceType(),
                                    affiliation.getLocalBadgeId(),
                                    gateName,
                                    timeStr),
                            "/daily-help",
                            ReferenceType.COMMUNITY,
                            affiliation.getId(),
                            NotificationPriority.NORMAL,
                            null,
                            request.getCommunityId()
                    );
                } catch (Exception ex) {
                    log.warn("Failed to notify resident {} of daily help entry: {}", contract.getResident().getId(), ex.getMessage());
                }
            }
        }

        return StaffGateScanResultDto.builder()
                .staffUuid(profile.getStaffUuid())
                .displayName(profile.getDisplayName())
                .primaryCategory(profile.getPrimaryCategory())
                .localBadgeId(affiliation.getLocalBadgeId())
                .gateState("IN")
                .eventTimestamp(eventTs)
                .notifiedResidentsCount(notifiedUsers.size())
                .notifiedFlatIds(notifiedFlatIds)
                .reconciledSessions(List.of())
                .message(String.format("%s checked IN at %s; notified %d contracted household(s)",
                        profile.getDisplayName(), gateName, notifiedUsers.size()))
                .build();
    }

    @Override
    @Transactional
    public StaffGateScanResultDto recordGateExit(StaffGateScanRequest request) {
        if (request.getCommunityId() == null || request.getStaffUuid() == null || request.getStaffUuid().isBlank()) {
            throw new InvalidInputException("communityId and staffUuid are required");
        }
        LocalDateTime exitTs = request.getEventTimestamp() != null ? request.getEventTimestamp() : LocalDateTime.now();
        String gateName = request.getGateName() != null && !request.getGateName().isBlank() ? request.getGateName() : "Main Gate";

        SocietyStaffAffiliation affiliation = affiliationRepo
                .findByCommunityIdAndStaffProfile_StaffUuid(request.getCommunityId(), request.getStaffUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Staff affiliation not found: " + request.getStaffUuid()));

        if ("OUT".equalsIgnoreCase(affiliation.getCurrentGateState())) {
            throw new InvalidInputException("Staff is already checked OUT of society gate");
        }

        LocalDateTime entryTs = affiliation.getLastGateEventAt() != null
                ? affiliation.getLastGateEventAt()
                : exitTs.minusMinutes(60);
        LocalTime entryTime = entryTs.toLocalDate().equals(exitTs.toLocalDate())
                ? entryTs.toLocalTime()
                : LocalTime.MIN;

        affiliation.setCurrentGateState("OUT");
        affiliation.setLastGateEventAt(exitTs);
        affiliationRepo.save(affiliation);

        List<FlatAttendanceReconciliationDto> reconciled = reconcileGateDwellSession(GateDwellIntervalDto.builder()
                .communityId(request.getCommunityId())
                .staffUuid(request.getStaffUuid())
                .workDate(exitTs.toLocalDate())
                .gateEntryTime(entryTime)
                .gateExitTime(exitTs.toLocalTime())
                .build());

        List<FlatStaffContract> contracts = contractRepo
                .findByCommunityIdAndAffiliation_StaffProfile_StaffUuidAndIsActiveTrue(
                        request.getCommunityId(), request.getStaffUuid());
        int dayOfWeekBit = 1 << (exitTs.getDayOfWeek().getValue() - 1);
        Set<Long> notifiedUsers = new LinkedHashSet<>();
        List<Long> notifiedFlatIds = new ArrayList<>();
        String timeStr = exitTs.format(DateTimeFormatter.ofPattern("hh:mm a"));
        GlobalStaffProfile profile = affiliation.getStaffProfile();

        for (FlatStaffContract contract : contracts) {
            if ((contract.getDaysOfWeekMask() & dayOfWeekBit) == 0) {
                continue;
            }
            notifiedFlatIds.add(contract.getFlatId());
            if (contract.getResident() != null && contract.getResident().getId() != null
                    && notifiedUsers.add(contract.getResident().getId()) && notificationService != null) {
                try {
                    notificationService.createNotification(
                            contract.getResident().getId(),
                            NotificationType.VISITOR_CHECK_OUT,
                            NotificationCategory.COMMUNITY,
                            "Daily Help Exited",
                            String.format("%s exited %s at %s.",
                                    profile.getDisplayName(), gateName, timeStr),
                            "/daily-help",
                            ReferenceType.COMMUNITY,
                            affiliation.getId(),
                            NotificationPriority.NORMAL,
                            null,
                            request.getCommunityId()
                    );
                } catch (Exception ex) {
                    log.warn("Failed to notify resident {} of daily help exit: {}", contract.getResident().getId(), ex.getMessage());
                }
            }
        }

        return StaffGateScanResultDto.builder()
                .staffUuid(profile.getStaffUuid())
                .displayName(profile.getDisplayName())
                .primaryCategory(profile.getPrimaryCategory())
                .localBadgeId(affiliation.getLocalBadgeId())
                .gateState("OUT")
                .eventTimestamp(exitTs)
                .notifiedResidentsCount(notifiedUsers.size())
                .notifiedFlatIds(notifiedFlatIds)
                .reconciledSessions(reconciled)
                .message(String.format("%s checked OUT at %s; reconciled %d flat attendance record(s)",
                        profile.getDisplayName(), gateName, reconciled.size()))
                .build();
    }

    @Override
    @Transactional
    public CrossSocietyStaffProfileDto registerMultiSocietyWorker(MultiSocietyAffiliationRequest request) {
        if (request.getCommunityId() == null || request.getRawPhone() == null || request.getRawPhone().isBlank()) {
            throw new InvalidInputException("communityId and rawPhone are required");
        }
        String normalizedPhone = request.getRawPhone().replaceAll("[^0-9]", "");
        if (normalizedPhone.length() < 6) {
            throw new InvalidInputException("Valid phone number is required");
        }
        String phoneHash = sha256Hex(normalizedPhone);
        String maskedPhone = "+91 " + normalizedPhone.substring(0, 2)
                + "****" + normalizedPhone.substring(normalizedPhone.length() - 4);

        GlobalStaffProfile profile = staffProfileRepo.findByPhoneHash(phoneHash)
                .orElseGet(() -> {
                    String uuid = request.getStaffUuid() != null && !request.getStaffUuid().isBlank()
                            ? request.getStaffUuid()
                            : "staff-" + UUID.randomUUID().toString().substring(0, 8);
                    return staffProfileRepo.save(GlobalStaffProfile.builder()
                            .staffUuid(uuid)
                            .phoneHash(phoneHash)
                            .maskedPhone(maskedPhone)
                            .displayName(request.getDisplayName() != null ? request.getDisplayName() : "Daily Help")
                            .primaryCategory(request.getPrimaryCategory() != null ? request.getPrimaryCategory() : "MAID")
                            .build());
                });

        affiliationRepo.findByCommunityIdAndStaffProfile_Id(request.getCommunityId(), profile.getId())
                .orElseGet(() -> {
                    Community comm = communityRepo != null
                            ? communityRepo.findById(request.getCommunityId())
                            .orElseGet(() -> Community.builder().id(request.getCommunityId()).name("Society-" + request.getCommunityId()).build())
                            : Community.builder().id(request.getCommunityId()).name("Society-" + request.getCommunityId()).build();
                    String badge = request.getLocalBadgeId() != null && !request.getLocalBadgeId().isBlank()
                            ? request.getLocalBadgeId()
                            : "DH-" + request.getCommunityId() + "-" + profile.getId();
                    return affiliationRepo.save(SocietyStaffAffiliation.builder()
                            .community(comm)
                            .staffProfile(profile)
                            .localBadgeId(badge)
                            .currentGateState("OUT")
                            .status("ACTIVE")
                            .build());
                });

        return getPublicCrossSocietyProfile(profile.getStaffUuid());
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
