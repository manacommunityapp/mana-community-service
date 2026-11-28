package com.manacommunity.api.unit.dailyhelp;

import com.manacommunity.api.dailyhelp.dto.DailyHelpDtos.*;
import com.manacommunity.api.dailyhelp.entity.*;
import com.manacommunity.api.dailyhelp.repository.DailyHelpRepositories.*;
import com.manacommunity.api.dailyhelp.service.impl.DailyHelpAttendanceServiceImpl;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.NotificationType;
import com.manacommunity.api.service.NotificationManagementService;
import com.manacommunity.api.staff.service.StaffAccessRuleService;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyHelpAttendanceServiceTest {

    @Mock private GlobalStaffProfileRepository staffProfileRepo;
    @Mock private SocietyStaffAffiliationRepository affiliationRepo;
    @Mock private FlatStaffContractRepository contractRepo;
    @Mock private FlatAttendanceSessionRepository attendanceRepo;
    @Mock private StaffMonthlySalaryLedgerRepository salaryRepo;

    @InjectMocks
    private DailyHelpAttendanceServiceImpl attendanceService;

    private Community communityGreenGlen;
    private GlobalStaffProfile cookProfile;
    private SocietyStaffAffiliation affiliationGreenGlen;
    private FlatStaffContract contractA101;
    private FlatStaffContract contractB202;

    @BeforeEach
    void setUp() {
        communityGreenGlen = Community.builder().id(101L).name("Green Glen").build();

        cookProfile = GlobalStaffProfile.builder()
                .id(1L)
                .staffUuid("cook-uuid-42")
                .displayName("Sunita Devi")
                .primaryCategory("COOK")
                .maskedPhone("+91 98****3210")
                .aggregateRating(new BigDecimal("4.85"))
                .ratingSampleSize(28)
                .build();

        affiliationGreenGlen = SocietyStaffAffiliation.builder()
                .id(11L)
                .community(communityGreenGlen)
                .staffProfile(cookProfile)
                .localBadgeId("GG-C-042")
                .build();

        AppUser resident1 = AppUser.builder().id(501L).fullName("Rahul Sharma").build();
        AppUser resident2 = AppUser.builder().id(502L).fullName("Anita Desai").build();

        // Flat A-101: 07:00 - 08:15 (75 min), stipend Rs. 4500/month, 7 days/week (mask=127)
        contractA101 = FlatStaffContract.builder()
                .id(201L)
                .community(communityGreenGlen)
                .flatId(1001L)
                .resident(resident1)
                .affiliation(affiliationGreenGlen)
                .serviceType("COOK_BREAKFAST")
                .daysOfWeekMask(127)
                .scheduledStartTime(LocalTime.of(7, 0))
                .scheduledEndTime(LocalTime.of(8, 15))
                .monthlyStipend(new BigDecimal("4500.00"))
                .leaveAllowancePerMonth(2)
                .build();

        // Flat B-202: 08:30 - 09:30 (60 min), stipend Rs. 4000/month, 7 days/week (mask=127)
        contractB202 = FlatStaffContract.builder()
                .id(202L)
                .community(communityGreenGlen)
                .flatId(1002L)
                .resident(resident2)
                .affiliation(affiliationGreenGlen)
                .serviceType("COOK_MORNING")
                .daysOfWeekMask(127)
                .scheduledStartTime(LocalTime.of(8, 30))
                .scheduledEndTime(LocalTime.of(9, 30))
                .monthlyStipend(new BigDecimal("4000.00"))
                .leaveAllowancePerMonth(2)
                .build();
    }

    @Test
    @DisplayName("Reconciles full and partial flat attendance from gate dwell window without leaking across flats")
    void reconcileGateDwellSession_calculatesProRataPerFlat() {
        // Cook entered Green Glen gate at 06:55 AM and departed at 09:00 AM (Dwell: 125 min)
        GateDwellIntervalDto dwell = GateDwellIntervalDto.builder()
                .communityId(101L)
                .staffUuid("cook-uuid-42")
                .workDate(LocalDate.of(2026, 11, 23)) // Monday
                .gateEntryTime(LocalTime.of(6, 55))
                .gateExitTime(LocalTime.of(9, 0))
                .build();

        when(contractRepo.findByCommunityIdAndAffiliation_StaffProfile_StaffUuidAndIsActiveTrue(101L, "cook-uuid-42"))
                .thenReturn(List.of(contractA101, contractB202));
        when(attendanceRepo.findByContract_IdAndWorkDate(any(), any())).thenReturn(Optional.empty());

        List<FlatAttendanceReconciliationDto> reconciled = attendanceService.reconcileGateDwellSession(dwell);

        assertThat(reconciled).hasSize(2);

        // Flat A-101: 07:00-08:15 (75m). Gate was open 06:55-09:00 -> 100% overlap -> PRESENT (multiplier 1.0)
        FlatAttendanceReconciliationDto recA = reconciled.stream()
                .filter(r -> r.getContractId().equals(201L)).findFirst().orElseThrow();
        assertThat(recA.getStatus()).isEqualTo("PRESENT");
        assertThat(recA.getActualDwellMinutes()).isEqualTo(75);
        assertThat(recA.getProRataMultiplier()).isEqualByComparingTo("1.00");

        // Flat B-202: 08:30-09:30 (60m). Cook exited at 09:00 -> overlap 08:30-09:00 (30m, 50% overlap) -> HALF_DAY (multiplier 0.5)
        FlatAttendanceReconciliationDto recB = reconciled.stream()
                .filter(r -> r.getContractId().equals(202L)).findFirst().orElseThrow();
        assertThat(recB.getStatus()).isEqualTo("HALF_DAY");
        assertThat(recB.getActualDwellMinutes()).isEqualTo(30);
        assertThat(recB.getProRataMultiplier()).isEqualByComparingTo("0.50");

        verify(attendanceRepo, times(2)).save(any(FlatAttendanceSession.class));
    }

    @Test
    @DisplayName("Calculates monthly pro-rata salary deductions accurately factoring in paid leave allowance")
    void calculateMonthlySalarySettlement_appliesLeaveAllowanceAndDeduction() {
        when(contractRepo.findById(201L)).thenReturn(Optional.of(contractA101));

        // November 2026: 30 calendar days
        // Suppose cook was present 24 days out of 30 scheduled days (6 days absent)
        // With 2 days allowed paid leave -> 4 unauthorized absences
        List<FlatAttendanceSession> sessions = List.of(
                FlatAttendanceSession.builder().attendanceStatus("PRESENT").build(),
                FlatAttendanceSession.builder().attendanceStatus("PRESENT").build(),
                FlatAttendanceSession.builder().attendanceStatus("PRESENT").build(),
                FlatAttendanceSession.builder().attendanceStatus("PRESENT").build()
        );
        when(attendanceRepo.findByContract_IdAndWorkDateBetween(eq(201L), any(), any()))
                .thenReturn(sessions);
        when(salaryRepo.findByContract_IdAndBillingMonth(201L, "2026-11")).thenReturn(Optional.empty());

        MonthlySalarySettlementDto settlement = attendanceService.calculateMonthlySalarySettlement(201L, "2026-11");

        assertThat(settlement.getContractId()).isEqualTo(201L);
        assertThat(settlement.getCalendarDays()).isEqualTo(30);
        assertThat(settlement.getScheduledWorkingDays()).isEqualTo(30);
        assertThat(settlement.getPresentDays()).isEqualTo(4);
        // Excess absences = (30 - 4) - 2 allowance = 24
        assertThat(settlement.getUnpaidAbsenceDays()).isEqualTo(24);

        // Daily rate = 4500 / 30 = 150/day. Deduction = 24 * 150 = 3600. Net = 900.
        assertThat(settlement.getProRataDeduction()).isEqualByComparingTo("3600.00");
        assertThat(settlement.getNetPayable()).isEqualByComparingTo("900.00");
    }

    @Test
    @DisplayName("Preserves zero-knowledge tenant isolation: queries flat history strictly by flatId")
    void getFlatAttendanceHistory_strictlyScopedToFlat() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 30);

        FlatAttendanceSession session = FlatAttendanceSession.builder()
                .contract(contractA101)
                .workDate(LocalDate.of(2026, 11, 23))
                .attendanceStatus("PRESENT")
                .scheduledMinutes(75)
                .societyDwellMinutes(75)
                .proRataMultiplier(new BigDecimal("1.00"))
                .build();

        when(attendanceRepo.findFlatAttendanceHistory(101L, 1001L, start, end))
                .thenReturn(List.of(session));

        List<FlatAttendanceReconciliationDto> history = attendanceService.getFlatAttendanceHistory(101L, 1001L, start, end);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getFlatId()).isEqualTo(1001L);
        verify(attendanceRepo).findFlatAttendanceHistory(101L, 1001L, start, end);
    }

    @Test
    @DisplayName("Gate entry notifies all scheduled flats, enforces curfews, and exit reconciles multi-flat dwell")
    void recordGateEntryAndExit_notifiesScheduledFlatsAndReconciles() {
        NotificationManagementService notifService = mock(NotificationManagementService.class);
        StaffAccessRuleService curfewService = mock(StaffAccessRuleService.class);
        ReflectionTestUtils.setField(attendanceService, "notificationService", notifService);
        ReflectionTestUtils.setField(attendanceService, "accessRuleService", curfewService);

        // Weekend-only flat should NOT receive Monday entry notification (mask 96 = Sat+Sun)
        AppUser weekendResident = AppUser.builder().id(503L).fullName("Weekend Owner").build();
        FlatStaffContract weekendOnly = FlatStaffContract.builder()
                .id(203L).community(communityGreenGlen).flatId(1003L).resident(weekendResident)
                .affiliation(affiliationGreenGlen).serviceType("COOK_WEEKEND").daysOfWeekMask(96)
                .scheduledStartTime(LocalTime.of(9, 0)).scheduledEndTime(LocalTime.of(10, 0))
                .monthlyStipend(new BigDecimal("2000.00")).build();

        LocalDateTime entryTs = LocalDateTime.of(2026, 11, 23, 6, 55); // Monday
        LocalDateTime exitTs = LocalDateTime.of(2026, 11, 23, 9, 0);

        when(affiliationRepo.findByCommunityIdAndStaffProfile_StaffUuid(101L, "cook-uuid-42"))
                .thenReturn(Optional.of(affiliationGreenGlen));
        when(curfewService.isAccessAllowed(eq(11L), eq("MONDAY"), any())).thenReturn(true);
        when(contractRepo.findByCommunityIdAndAffiliation_StaffProfile_StaffUuidAndIsActiveTrue(101L, "cook-uuid-42"))
                .thenReturn(List.of(contractA101, contractB202, weekendOnly));
        when(attendanceRepo.findByContract_IdAndWorkDate(any(), any())).thenReturn(Optional.empty());

        StaffGateScanResultDto entryRes = attendanceService.recordGateEntry(StaffGateScanRequest.builder()
                .communityId(101L).staffUuid("cook-uuid-42").gateName("East Gate").eventTimestamp(entryTs).build());

        assertThat(entryRes.getGateState()).isEqualTo("IN");
        assertThat(entryRes.getNotifiedResidentsCount()).isEqualTo(2);
        assertThat(entryRes.getNotifiedFlatIds()).containsExactly(1001L, 1002L);
        verify(notifService).createNotification(eq(501L), eq(NotificationType.VISITOR_CHECK_IN), any(),
                eq("Daily Help Arrived"), contains("East Gate"), any(), any(), eq(11L), any(), any(), eq(101L));
        verify(notifService).createNotification(eq(502L), eq(NotificationType.VISITOR_CHECK_IN), any(),
                eq("Daily Help Arrived"), contains("East Gate"), any(), any(), eq(11L), any(), any(), eq(101L));
        verify(notifService, never()).createNotification(eq(503L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        StaffGateScanResultDto exitRes = attendanceService.recordGateExit(StaffGateScanRequest.builder()
                .communityId(101L).staffUuid("cook-uuid-42").gateName("East Gate").eventTimestamp(exitTs).build());

        assertThat(exitRes.getGateState()).isEqualTo("OUT");
        assertThat(exitRes.getReconciledSessions()).hasSize(2);
        verify(notifService).createNotification(eq(501L), eq(NotificationType.VISITOR_CHECK_OUT), any(),
                eq("Daily Help Exited"), contains("East Gate"), any(), any(), eq(11L), any(), any(), eq(101L));

        // Curfew violation after allowed window must block entry
        when(curfewService.isAccessAllowed(eq(11L), eq("MONDAY"), eq(LocalTime.of(23, 30)))).thenReturn(false);
        assertThatThrownBy(() -> attendanceService.recordGateEntry(StaffGateScanRequest.builder()
                .communityId(101L).staffUuid("cook-uuid-42").gateName("Main Gate")
                .eventTimestamp(LocalDateTime.of(2026, 11, 23, 23, 30)).build()))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    @DisplayName("Registers global worker once by phone hash and affiliates across multiple societies")
    void registerMultiSocietyWorker_sharesGlobalProfileAcrossSocieties() {
        when(staffProfileRepo.findByPhoneHash(anyString())).thenReturn(Optional.empty());
        when(staffProfileRepo.save(any(GlobalStaffProfile.class))).thenAnswer(inv -> {
            GlobalStaffProfile p = inv.getArgument(0);
            p.setId(55L);
            return p;
        });
        when(staffProfileRepo.findByStaffUuid(anyString())).thenAnswer(inv -> Optional.of(GlobalStaffProfile.builder()
                .id(55L).staffUuid(inv.getArgument(0)).displayName("Raju Driver").primaryCategory("DRIVER")
                .maskedPhone("+91 98****4321").aggregateRating(new BigDecimal("5.00")).ratingSampleSize(0).build()));
        when(affiliationRepo.findByCommunityIdAndStaffProfile_Id(102L, 55L)).thenReturn(Optional.empty());
        when(affiliationRepo.findByStaffProfile_StaffUuid(anyString())).thenReturn(List.of(
                SocietyStaffAffiliation.builder().community(communityGreenGlen).build(),
                SocietyStaffAffiliation.builder().community(Community.builder().id(102L).name("Palm Meadows").build()).build()
        ));

        CrossSocietyStaffProfileDto profile = attendanceService.registerMultiSocietyWorker(
                MultiSocietyAffiliationRequest.builder()
                        .staffUuid("driver-55")
                        .displayName("Raju Driver")
                        .primaryCategory("DRIVER")
                        .rawPhone("9876544321")
                        .communityId(102L)
                        .localBadgeId("PM-D-055")
                        .build());

        assertThat(profile.getMaskedPhone()).isEqualTo("+91 98****4321");
        assertThat(profile.getAffiliatedCommunities()).containsExactly("Green Glen", "Palm Meadows");
    }
}
