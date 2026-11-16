package com.manacommunity.api.socialcircle.service;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.notification.orchestrator.UnifiedNotificationService;
import com.manacommunity.api.repository.NotificationRepository;
import com.manacommunity.api.socialcircle.entity.KittyPartyCircle;
import com.manacommunity.api.socialcircle.entity.KittyPartyContribution;
import com.manacommunity.api.socialcircle.entity.KittyPartyCycle;
import com.manacommunity.api.socialcircle.entity.KittyPartyMember;
import com.manacommunity.api.socialcircle.repository.KittyPartyContributionRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyCycleRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KittyPartyReminderSchedulerTest {

    @Mock
    private KittyPartyCycleRepository cycleRepository;

    @Mock
    private KittyPartyContributionRepository contributionRepository;

    @Mock
    private KittyPartyMemberRepository memberRepository;

    @Mock
    private KittyPartyLedgerService ledgerService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UnifiedNotificationService unifiedNotificationService;

    @InjectMocks
    private KittyPartyReminderScheduler reminderScheduler;

    private KittyPartyCycle cycle;
    private AppUser member1;
    private AppUser member2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reminderScheduler, "unifiedNotificationService", unifiedNotificationService);

        Community community = Community.builder().id(1L).name("Mana Residency").build();
        KittyPartyCircle circle = KittyPartyCircle.builder().id(10L).community(community).circleName("Gold Circle").build();

        cycle = KittyPartyCycle.builder()
                .id(100L)
                .circle(circle)
                .cycleName("Year 2026 Gold Cycle")
                .contributionAmount(BigDecimal.valueOf(5000))
                .frequency("MONTHLY")
                .memberCount(10)
                .startDate(LocalDate.now().minusMonths(1))
                .status("ACTIVE")
                .build();

        member1 = AppUser.builder().id(21L).fullName("Pooja Sharma").phone("9876543210").community(community).build();
        member2 = AppUser.builder().id(22L).fullName("Kavita Rao").phone("9876543211").community(community).build();
    }

    @Test
    void runAutomatedReminders_noActiveCycles_returnsZero() {
        when(cycleRepository.findByStatus("ACTIVE")).thenReturn(Collections.emptyList());

        int count = reminderScheduler.runAutomatedReminders();

        assertEquals(0, count);
        verifyNoInteractions(memberRepository, contributionRepository, notificationRepository);
    }

    @Test
    void runAutomatedReminders_dueInThreeDays_dispatchesToUnpaidMembers() {
        when(cycleRepository.findByStatus("ACTIVE")).thenReturn(List.of(cycle));
        when(ledgerService.computeCurrentPeriod(cycle)).thenReturn(2);
        // Due in exactly 3 days
        LocalDate dueInThreeDays = LocalDate.now().plusDays(3);
        when(ledgerService.computePeriodDueDate(cycle.getStartDate(), 2)).thenReturn(dueInThreeDays);

        KittyPartyMember m1 = KittyPartyMember.builder().user(member1).circle(cycle.getCircle()).membershipStatus("ACTIVE").build();
        KittyPartyMember m2 = KittyPartyMember.builder().user(member2).circle(cycle.getCircle()).membershipStatus("ACTIVE").build();
        when(memberRepository.findByCircleIdAndMembershipStatus(10L, "ACTIVE")).thenReturn(List.of(m1, m2));

        // Member 1 already paid, Member 2 unpaid
        KittyPartyContribution c1 = KittyPartyContribution.builder()
                .cycle(cycle)
                .memberUser(member1)
                .periodNumber(2)
                .paymentStatus("PAID")
                .recordedAmount(BigDecimal.valueOf(5000))
                .build();
        when(contributionRepository.findByCycleIdAndPeriodNumber(100L, 2)).thenReturn(List.of(c1));

        int count = reminderScheduler.runAutomatedReminders();

        assertEquals(1, count);
        verify(notificationRepository, times(1)).save(any());
        verify(unifiedNotificationService, times(1)).orchestrate(any());
    }

    @Test
    void runAutomatedReminders_allMembersPaid_dispatchesZero() {
        when(cycleRepository.findByStatus("ACTIVE")).thenReturn(List.of(cycle));
        when(ledgerService.computeCurrentPeriod(cycle)).thenReturn(2);
        LocalDate dueToday = LocalDate.now();
        when(ledgerService.computePeriodDueDate(cycle.getStartDate(), 2)).thenReturn(dueToday);

        KittyPartyMember m1 = KittyPartyMember.builder().user(member1).circle(cycle.getCircle()).membershipStatus("ACTIVE").build();
        when(memberRepository.findByCircleIdAndMembershipStatus(10L, "ACTIVE")).thenReturn(List.of(m1));

        KittyPartyContribution c1 = KittyPartyContribution.builder()
                .cycle(cycle)
                .memberUser(member1)
                .periodNumber(2)
                .paymentStatus("VERIFIED")
                .recordedAmount(BigDecimal.valueOf(5000))
                .build();
        when(contributionRepository.findByCycleIdAndPeriodNumber(100L, 2)).thenReturn(List.of(c1));

        int count = reminderScheduler.runAutomatedReminders();

        assertEquals(0, count);
        verify(notificationRepository, never()).save(any());
        verify(unifiedNotificationService, never()).orchestrate(any());
    }
}
