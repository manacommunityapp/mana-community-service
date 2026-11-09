package com.manacommunity.api.socialcircle.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.NotificationRepository;
import com.manacommunity.api.socialcircle.dto.KittyPartyLedgerDtos.*;
import com.manacommunity.api.socialcircle.entity.*;
import com.manacommunity.api.socialcircle.repository.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kitty Party Advanced Features Test Suite")
class KittyPartyAdvancedFeaturesTest {

    @Mock
    private KittyPartyCycleRepository cycleRepository;
    @Mock
    private KittyPartyContributionRepository contributionRepository;
    @Mock
    private KittyPartyRotationRepository rotationRepository;
    @Mock
    private KittyPartyCircleRepository circleRepository;
    @Mock
    private KittyPartyMemberRepository memberRepository;
    @Mock
    private KittyPartyAuditLogRepository auditLogRepository;
    @Mock
    private KittyPartyDisputeRepository disputeRepository;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private KittyPartyLedgerService ledgerService;

    private AppUser adminUser;
    private AppUser memberUser2;
    private AppUser memberUser3;
    private KittyPartyCircle circle;
    private KittyPartyCycle cycle;

    @BeforeEach
    void setUp() {
        adminUser = new AppUser();
        adminUser.setId(1L);
        adminUser.setEmail("admin@example.com");
        adminUser.setFullName("Admin User");

        memberUser2 = new AppUser();
        memberUser2.setId(2L);
        memberUser2.setEmail("member2@example.com");
        memberUser2.setFullName("Member Two");

        memberUser3 = new AppUser();
        memberUser3.setId(3L);
        memberUser3.setEmail("member3@example.com");
        memberUser3.setFullName("Member Three");

        Community community = new Community();
        community.setId(50L);

        circle = KittyPartyCircle.builder()
                .id(10L)
                .community(community)
                .circleName("Golden Kitties")
                .status("ACTIVE")
                .createdBy(adminUser)
                .build();

        cycle = KittyPartyCycle.builder()
                .id(100L)
                .circle(circle)
                .cycleName("Golden Kitty 2026")
                .contributionAmount(new BigDecimal("2000.00"))
                .frequency("MONTHLY")
                .memberCount(3)
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(LocalDate.now().plusMonths(2))
                .financialMode("RECORD_ONLY")
                .status("ACTIVE")
                .createdBy(adminUser)
                .build();
    }

    @Test
    @DisplayName("Feature 1: Auto-sequence fixed rotation order when user list is omitted")
    void testAutoSequenceFixedRotationOrder() {
        KittyPartyMember mAdmin = KittyPartyMember.builder().circle(circle).user(adminUser).roleCode("ADMIN").membershipStatus("ACTIVE").build();
        KittyPartyMember mM2 = KittyPartyMember.builder().circle(circle).user(memberUser2).roleCode("MEMBER").membershipStatus("ACTIVE").build();
        KittyPartyMember mM3 = KittyPartyMember.builder().circle(circle).user(memberUser3).roleCode("MEMBER").membershipStatus("ACTIVE").build();

        when(memberRepository.findByCircleIdAndUserId(10L, 1L)).thenReturn(Optional.of(mAdmin));
        when(circleRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(circle));
        when(memberRepository.findByCircleIdAndMembershipStatus(10L, "ACTIVE")).thenReturn(List.of(mAdmin, mM2, mM3));
        when(cycleRepository.save(any(KittyPartyCycle.class))).thenReturn(cycle);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(memberUser2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(memberUser3));
        when(cycleRepository.findById(100L)).thenReturn(Optional.of(cycle));

        CreateCycleRequest req = CreateCycleRequest.builder()
                .cycleName("Golden Kitty 2026")
                .contributionAmount(new BigDecimal("2000.00"))
                .memberCount(3)
                .startDate(LocalDate.now())
                .build();

        CycleSummaryResponse res = ledgerService.createCycle(10L, adminUser, req);
        assertNotNull(res);
        // Verify rotationRepository.save was invoked 3 times for periods 1, 2, 3
        verify(rotationRepository, times(3)).save(any(KittyPartyRotation.class));
        // Verify audit log was recorded
        verify(auditLogRepository, times(1)).save(any(KittyPartyAuditLog.class));
    }

    @Test
    @DisplayName("Feature 2: Admin swap rotation updates status to SWAPPED and creates audit entry")
    void testAdminSwapRotation() {
        KittyPartyMember mAdmin = KittyPartyMember.builder().circle(circle).user(adminUser).roleCode("ADMIN").membershipStatus("ACTIVE").build();
        when(cycleRepository.findById(100L)).thenReturn(Optional.of(cycle));
        when(memberRepository.findByCircleIdAndUserId(10L, 1L)).thenReturn(Optional.of(mAdmin));

        KittyPartyRotation rot1 = KittyPartyRotation.builder()
                .id(501L).cycle(cycle).beneficiaryUser(memberUser2).periodNumber(1).rotationStatus("PLANNED").build();
        KittyPartyRotation rot2 = KittyPartyRotation.builder()
                .id(502L).cycle(cycle).beneficiaryUser(memberUser3).periodNumber(2).rotationStatus("PLANNED").build();

        when(rotationRepository.findByCycleIdAndPeriodNumber(100L, 1)).thenReturn(Optional.of(rot1));
        when(rotationRepository.findByCycleIdAndPeriodNumber(100L, 2)).thenReturn(Optional.of(rot2));

        SwapRotationRequest req = SwapRotationRequest.builder()
                .fromPeriod(1)
                .toPeriod(2)
                .reason("Member 2 requested earlier turn for medical reasons")
                .build();

        CycleSummaryResponse res = ledgerService.adminSwapRotation(100L, adminUser, req);
        assertNotNull(res);

        assertEquals("SWAPPED", rot1.getRotationStatus());
        assertEquals("SWAPPED", rot2.getRotationStatus());
        assertEquals(2, rot1.getPeriodNumber());
        assertEquals(1, rot2.getPeriodNumber());
        verify(auditLogRepository, times(1)).save(any(KittyPartyAuditLog.class));
    }

    @Test
    @DisplayName("Feature 3: Send payment reminders dispatches notifications to pending members")
    void testSendPaymentReminders() {
        KittyPartyMember mAdmin = KittyPartyMember.builder().circle(circle).user(adminUser).roleCode("ADMIN").membershipStatus("ACTIVE").build();
        KittyPartyMember mM2 = KittyPartyMember.builder().circle(circle).user(memberUser2).roleCode("MEMBER").membershipStatus("ACTIVE").build();

        when(cycleRepository.findById(100L)).thenReturn(Optional.of(cycle));
        when(memberRepository.findByCircleIdAndUserId(10L, 1L)).thenReturn(Optional.of(mAdmin));
        when(memberRepository.findByCircleIdAndMembershipStatus(10L, "ACTIVE")).thenReturn(List.of(mAdmin, mM2));
        when(contributionRepository.findByCycleIdAndPeriodNumber(100L, 1)).thenReturn(Collections.emptyList());

        SendRemindersResponse res = ledgerService.sendPaymentReminders(100L, adminUser, 1);
        assertEquals(2, res.getRemindedCount());
        assertTrue(res.getRemindedMembers().contains("Member Two"));
        verify(notificationRepository, times(2)).save(any());
        verify(auditLogRepository, times(1)).save(any(KittyPartyAuditLog.class));
    }

    @Test
    @DisplayName("Feature 4: Late payment tracking correctly flags overdue contributions")
    void testLatePaymentTracking() {
        LocalDate cycleStart = LocalDate.now().minusMonths(2);
        KittyPartyCycle oldCycle = KittyPartyCycle.builder()
                .id(100L)
                .circle(circle)
                .cycleName("Test Cycle")
                .contributionAmount(new BigDecimal("1000.00"))
                .memberCount(3)
                .startDate(cycleStart)
                .financialMode("RECORD_ONLY")
                .status("ACTIVE")
                .build();

        when(cycleRepository.findById(100L)).thenReturn(Optional.of(oldCycle));

        // Period 1 is 2 months ago, status PENDING -> overdue!
        KittyPartyContribution pendingContrib = KittyPartyContribution.builder()
                .id(1L)
                .cycle(oldCycle)
                .memberUser(memberUser2)
                .periodNumber(1)
                .expectedAmount(new BigDecimal("1000.00"))
                .paymentStatus("PENDING")
                .build();

        when(contributionRepository.findByCycleIdAndPeriodNumber(100L, 1)).thenReturn(List.of(pendingContrib));

        List<ContributionEntryDto> contribs = ledgerService.getContributionsForPeriod(100L, 1);
        assertEquals(1, contribs.size());
        assertTrue(contribs.get(0).getIsOverdue());
        assertTrue(contribs.get(0).getDaysOverdue() > 20);
    }

    @Test
    @DisplayName("Feature 5: Member replacement updates future rotations and audit log")
    void testMemberReplacement() {
        KittyPartyMember mAdmin = KittyPartyMember.builder().circle(circle).user(adminUser).roleCode("ADMIN").membershipStatus("ACTIVE").build();
        KittyPartyMember mOld = KittyPartyMember.builder().circle(circle).user(memberUser2).roleCode("MEMBER").membershipStatus("ACTIVE").build();

        when(cycleRepository.findById(100L)).thenReturn(Optional.of(cycle));
        when(memberRepository.findByCircleIdAndUserId(10L, 1L)).thenReturn(Optional.of(mAdmin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(memberUser2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(memberUser3));
        when(memberRepository.findByCircleIdAndUserId(10L, 2L)).thenReturn(Optional.of(mOld));
        when(memberRepository.findByCircleIdAndUserId(10L, 3L)).thenReturn(Optional.empty());

        KittyPartyRotation futureRotation = KittyPartyRotation.builder()
                .id(701L).cycle(cycle).beneficiaryUser(memberUser2).periodNumber(2).rotationStatus("PLANNED").build();
        when(rotationRepository.findByCycleIdOrderByPeriodNumberAsc(100L)).thenReturn(List.of(futureRotation));

        ReplaceMemberRequest req = ReplaceMemberRequest.builder()
                .oldUserId(2L)
                .newUserId(3L)
                .reason("Member moved out of community")
                .build();

        ledgerService.withdrawAndReplaceMember(100L, adminUser, req);

        assertEquals("LEFT", mOld.getMembershipStatus());
        assertEquals(memberUser3, futureRotation.getBeneficiaryUser());
        assertEquals("SWAPPED", futureRotation.getRotationStatus());
        verify(auditLogRepository, times(1)).save(any(KittyPartyAuditLog.class));
    }

    @Test
    @DisplayName("Feature 6: Dispute creation and resolution")
    void testDisputeManagement() {
        when(circleRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(circle));
        when(cycleRepository.findById(100L)).thenReturn(Optional.of(cycle));

        CreateDisputeRequest req = CreateDisputeRequest.builder()
                .cycleId(100L)
                .disputeType("AMOUNT_MISMATCH")
                .title("Incorrect recorded amount")
                .description("I paid 2000 but only 1000 was acknowledged")
                .build();

        when(disputeRepository.save(any(KittyPartyDispute.class))).thenAnswer(i -> {
            KittyPartyDispute d = i.getArgument(0);
            d.setId(88L);
            return d;
        });

        DisputeResponse res = ledgerService.createDispute(10L, memberUser2, req);
        assertNotNull(res);
        assertEquals("AMOUNT_MISMATCH", res.getDisputeType());
        assertEquals("OPEN", res.getStatus());
        verify(auditLogRepository, times(1)).save(any(KittyPartyAuditLog.class));

        // Resolution
        KittyPartyMember mAdmin = KittyPartyMember.builder().circle(circle).user(adminUser).roleCode("ADMIN").membershipStatus("ACTIVE").build();
        when(memberRepository.findByCircleIdAndUserId(10L, 1L)).thenReturn(Optional.of(mAdmin));

        KittyPartyDispute existingDispute = KittyPartyDispute.builder()
                .id(88L).circle(circle).cycle(cycle).raisedBy(memberUser2).disputeType("AMOUNT_MISMATCH").title("Dispute").description("Desc").status("OPEN").build();
        when(disputeRepository.findById(88L)).thenReturn(Optional.of(existingDispute));

        ResolveDisputeRequest resolveReq = ResolveDisputeRequest.builder()
                .status("RESOLVED")
                .resolutionNotes("Admin verified bank statement and credited full 2000.")
                .build();

        DisputeResponse resolved = ledgerService.resolveDispute(10L, 88L, adminUser, resolveReq);
        assertEquals("RESOLVED", resolved.getStatus());
        assertEquals("Admin verified bank statement and credited full 2000.", resolved.getResolutionNotes());
    }

    @Test
    @DisplayName("Feature 7: Downloadable monthly statement produces CSV and JSON")
    void testMonthlyStatement() {
        when(cycleRepository.findById(100L)).thenReturn(Optional.of(cycle));
        KittyPartyRotation rot = KittyPartyRotation.builder()
                .cycle(cycle).beneficiaryUser(memberUser2).periodNumber(1).rotationStatus("PLANNED").build();
        when(rotationRepository.findByCycleIdAndPeriodNumber(100L, 1)).thenReturn(Optional.of(rot));
        when(contributionRepository.findByCycleIdAndPeriodNumber(100L, 1)).thenReturn(Collections.emptyList());

        MonthlyStatementResponse jsonRes = ledgerService.generateMonthlyStatement(100L, 1);
        assertNotNull(jsonRes);
        assertEquals("Golden Kitties", jsonRes.getCircleName());
        assertTrue(jsonRes.getLegalDisclaimer().contains("Chit Funds Act, 1982"));

        String csvRes = ledgerService.generateStatementCsv(100L, 1);
        assertNotNull(csvRes);
        assertTrue(csvRes.contains("Mana Kitty Party - Monthly Ledger Statement"));
        assertTrue(csvRes.contains("Member ID,Member Name,Expected (INR)"));
    }

    @Test
    @DisplayName("Feature 8: SHA-256 hash chaining links consecutive audit log records")
    void testAuditHashChaining() {
        when(cycleRepository.findById(100L)).thenReturn(Optional.of(cycle));
        when(auditLogRepository.findTopByCycleIdOrderByIdDesc(100L)).thenReturn(Optional.empty());

        when(auditLogRepository.save(any(KittyPartyAuditLog.class))).thenAnswer(i -> {
            KittyPartyAuditLog a = i.getArgument(0);
            a.setId(1L);
            return a;
        });

        KittyPartyAuditLog firstLog = ledgerService.recordAuditLog(
                100L, 10L, "CYCLE_CREATED", adminUser, "Cycle created", "{}"
        );

        assertNotNull(firstLog);
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", firstLog.getPrevHash());
        assertNotNull(firstLog.getCurrHash());
        assertEquals(64, firstLog.getCurrHash().length()); // SHA-256 hex length
    }
}
