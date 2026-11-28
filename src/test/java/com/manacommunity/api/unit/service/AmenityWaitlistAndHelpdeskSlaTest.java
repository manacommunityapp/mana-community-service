package com.manacommunity.api.unit.service;

import com.manacommunity.api.amenity.entity.BookingWaitlist;
import com.manacommunity.api.amenity.entity.Facility;
import com.manacommunity.api.amenity.entity.FacilitySlot;
import com.manacommunity.api.amenity.entity.FlatBookingQuota;
import com.manacommunity.api.amenity.repository.BookingWaitlistRepository;
import com.manacommunity.api.amenity.repository.FacilityRepository;
import com.manacommunity.api.amenity.repository.FacilitySlotRepository;
import com.manacommunity.api.amenity.repository.FlatBookingQuotaRepository;
import com.manacommunity.api.amenity.service.SlotBookingService;
import com.manacommunity.api.amenity.service.WaitlistPromotionService;
import com.manacommunity.api.helpdesk.entity.EscalationRule;
import com.manacommunity.api.helpdesk.entity.HelpdeskTicket;
import com.manacommunity.api.helpdesk.repository.EscalationRuleRepository;
import com.manacommunity.api.helpdesk.repository.HelpdeskTicketRepository;
import com.manacommunity.api.helpdesk.service.SlaEscalationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AmenityWaitlistAndHelpdeskSlaTest {

    @Mock private FacilityRepository facilityRepo;
    @Mock private FacilitySlotRepository slotRepo;
    @Mock private FlatBookingQuotaRepository quotaRepo;
    @Mock private BookingWaitlistRepository waitlistRepo;
    @Mock private HelpdeskTicketRepository ticketRepo;
    @Mock private EscalationRuleRepository ruleRepo;

    private SlotBookingService bookingService;
    private WaitlistPromotionService waitlistService;
    private SlaEscalationService slaService;

    private final Map<Long, FacilitySlot> slotStore = new ConcurrentHashMap<>();
    private final Map<String, FlatBookingQuota> quotaStore = new ConcurrentHashMap<>();
    private final Map<String, BookingWaitlist> waitlistStore = new ConcurrentHashMap<>();
    private final Map<Long, HelpdeskTicket> ticketStore = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(100L);

    @BeforeEach
    void setUp() {
        slotStore.clear();
        quotaStore.clear();
        waitlistStore.clear();
        ticketStore.clear();

        Facility court = Facility.builder()
                .id(10L).communityId(412L).facilityCode("TENNIS-1").name("Tennis Court")
                .facilityType("TENNIS").slotDurationMinutes(60).openHour(6).closeHour(22)
                .peakStartHour(18).peakEndHour(21).maxBookingsPerFlatPerWeek(3).maxConcurrentPeakBookings(1)
                .active(true).build();
        lenient().when(facilityRepo.findById(10L)).thenReturn(Optional.of(court));

        FacilitySlot slot = FacilitySlot.builder()
                .id(55L).facilityId(10L).communityId(412L).slotDate(LocalDate.of(2026, 12, 1))
                .startTime(LocalTime.of(19, 0)).endTime(LocalTime.of(20, 0))
                .status("AVAILABLE").isPeakHour(true).build();
        slotStore.put(55L, slot);

        lenient().when(slotRepo.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(slotStore.get(inv.getArgument(0))));
        lenient().when(slotRepo.findByIdWithPessimisticLock(anyLong())).thenAnswer(inv -> Optional.ofNullable(slotStore.get(inv.getArgument(0))));
        lenient().when(slotRepo.save(any())).thenAnswer(inv -> {
            FacilitySlot s = inv.getArgument(0);
            slotStore.put(s.getId(), s);
            return s;
        });

        lenient().when(quotaRepo.findForUpdate(anyLong(), anyString(), anyLong(), any())).thenAnswer(inv ->
                Optional.ofNullable(quotaStore.get(inv.getArgument(1))));
        lenient().when(quotaRepo.findByCommunityIdAndFlatNoAndFacilityIdAndWeekStart(anyLong(), anyString(), anyLong(), any()))
                .thenAnswer(inv -> Optional.ofNullable(quotaStore.get(inv.getArgument(1))));
        lenient().when(quotaRepo.save(any())).thenAnswer(inv -> {
            FlatBookingQuota q = inv.getArgument(0);
            quotaStore.put(q.getFlatNo(), q);
            return q;
        });

        lenient().when(waitlistRepo.findBySlotIdAndResidentId(anyLong(), anyLong())).thenAnswer(inv ->
                Optional.ofNullable(waitlistStore.get(inv.getArgument(0) + ":" + inv.getArgument(1))));
        lenient().when(waitlistRepo.findMaxPosition(anyLong())).thenAnswer(inv -> {
            Long sId = inv.getArgument(0);
            return waitlistStore.values().stream()
                    .filter(w -> sId.equals(w.getSlotId()))
                    .map(BookingWaitlist::getPosition)
                    .max(Integer::compareTo);
        });
        lenient().when(waitlistRepo.findBySlotIdAndStatusOrderByPositionAsc(anyLong(), anyString())).thenAnswer(inv -> {
            Long sId = inv.getArgument(0);
            String st = inv.getArgument(1);
            return waitlistStore.values().stream()
                    .filter(w -> sId.equals(w.getSlotId()) && st.equals(w.getStatus()))
                    .sorted(Comparator.comparingInt(BookingWaitlist::getPosition))
                    .toList();
        });
        lenient().when(waitlistRepo.save(any())).thenAnswer(inv -> {
            BookingWaitlist w = inv.getArgument(0);
            if (w.getId() == null) w.setId(seq.getAndIncrement());
            waitlistStore.put(w.getSlotId() + ":" + w.getResidentId(), w);
            return w;
        });

        EscalationRule level1 = EscalationRule.builder()
                .id(1L).communityId(412L).category("PLUMBING").escalationLevel(1)
                .escalateAfterMinutes(60).escalateToRole("FACILITY_MANAGER")
                .pauseDuringOffHours(true).offHoursStart(22).offHoursEnd(7).active(true).build();
        EscalationRule level2 = EscalationRule.builder()
                .id(2L).communityId(412L).category("PLUMBING").escalationLevel(2)
                .escalateAfterMinutes(120).escalateToRole("ESTATE_HEAD")
                .pauseDuringOffHours(true).offHoursStart(22).offHoursEnd(7).active(true).build();
        lenient().when(ruleRepo.findByCommunityIdAndCategoryAndActiveTrueOrderByEscalationLevelAsc(412L, "PLUMBING"))
                .thenReturn(List.of(level1, level2));

        lenient().when(ticketRepo.save(any())).thenAnswer(inv -> {
            HelpdeskTicket t = inv.getArgument(0);
            if (t.getId() == null) t.setId(seq.getAndIncrement());
            ticketStore.put(t.getId(), t);
            return t;
        });
        lenient().when(ticketRepo.findByStatusIn(any())).thenAnswer(inv -> {
            List<String> sts = inv.getArgument(0);
            return ticketStore.values().stream().filter(t -> sts.contains(t.getStatus())).toList();
        });
        lenient().when(ticketRepo.findBreachedTickets(any())).thenAnswer(inv -> {
            Instant now = inv.getArgument(0);
            return ticketStore.values().stream()
                    .filter(t -> List.of("OPEN", "IN_PROGRESS").contains(t.getStatus()))
                    .filter(t -> !Boolean.TRUE.equals(t.getSlaPaused()))
                    .filter(t -> t.getSlaDeadline() != null && t.getSlaDeadline().isBefore(now))
                    .toList();
        });

        bookingService = new SlotBookingService(facilityRepo, slotRepo, quotaRepo, waitlistRepo);
        waitlistService = new WaitlistPromotionService(waitlistRepo, slotRepo, bookingService);
        slaService = new SlaEscalationService(ticketRepo, ruleRepo);
    }

    @Test
    void preventsDoubleBookingAndAtomicallyHoldsCancelledSlotForWaitlistWithExpiredOfferCascade() {
        var r1 = bookingService.bookSlot(55L, 1001L, "A-101");
        assertThat(r1.success()).isTrue();

        // Concurrent booking by Resident 2 fails while BOOKED
        var concurrentFail = bookingService.bookSlot(55L, 1002L, "B-202");
        assertThat(concurrentFail.success()).isFalse();

        // Residents 2 and 3 join waitlist
        var w1 = waitlistService.joinWaitlist(55L, 1002L, "B-202");
        var w2 = waitlistService.joinWaitlist(55L, 1003L, "C-303");
        assertThat(w1.position()).isEqualTo(1);
        assertThat(w2.position()).isEqualTo(2);

        // Resident 1 cancels -> slot moves straight to HELD & #1 gets OFFERED atomically
        var cancel = bookingService.cancelBooking(55L, 1001L);
        assertThat(cancel.success()).isTrue();
        assertThat(cancel.promotedWaitlistResidentId()).isEqualTo(1002L);
        assertThat(cancel.promotedWaitlistPosition()).isEqualTo(1);
        assertThat(slotStore.get(55L).getStatus()).isEqualTo("HELD");

        // Random Resident 4 tries sneaking in while HELD -> blocked
        assertThat(bookingService.bookSlot(55L, 1004L, "D-404").success()).isFalse();

        // Simulate #1's 15m offer expiring -> acceptOffer expires #1 and cascades hold immediately to #2!
        BookingWaitlist entry1 = waitlistStore.get("55:1002");
        entry1.setOfferExpiresAt(Instant.now().minusSeconds(30));
        var expiredAttempt = waitlistService.acceptOffer(55L, 1002L);
        assertThat(expiredAttempt.success()).isFalse();
        assertThat(waitlistStore.get("55:1002").getStatus()).isEqualTo("EXPIRED");
        assertThat(waitlistStore.get("55:1003").getStatus()).isEqualTo("OFFERED");

        // Waitlist #2 accepts within window -> succeeds cleanly!
        var promotedBooking = waitlistService.acceptOffer(55L, 1003L);
        assertThat(promotedBooking.success()).isTrue();
        assertThat(slotStore.get(55L).getBookedByResidentId()).isEqualTo(1003L);
    }

    @Test
    void validatesComplaintAttachmentsAndSupportsOffHoursSlaPauseResumeAndEscalation() {
        assertThatThrownBy(() -> SlaEscalationService.sanitizeAttachmentUrls("javascript:alert(1)"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SlaEscalationService.sanitizeAttachmentUrls(
                "https://a/1.jpg,https://a/2.jpg,https://a/3.jpg,https://a/4.jpg,https://a/5.jpg,https://a/6.jpg"))
                .isInstanceOf(IllegalArgumentException.class);

        HelpdeskTicket t = HelpdeskTicket.builder()
                .communityId(412L).residentId(77L).flatNo("B-404").tower("B")
                .title("Bathroom pipe leak").category("PLUMBING").priority("MEDIUM")
                .attachmentUrls(" https://cdn.mygate.in/leak1.jpg , https://cdn.mygate.in/leak2.jpg ")
                .build();
        t.setCreatedAt(LocalDateTime.now().minusMinutes(90));
        HelpdeskTicket saved = slaService.createTicket(t);
        assertThat(saved.getAttachmentUrls()).isEqualTo("https://cdn.mygate.in/leak1.jpg,https://cdn.mygate.in/leak2.jpg");

        // Force non-paused active state at T0 with deadline +60m, then run off-hours pause at 23:00 IST
        saved.setSlaPaused(false);
        Instant tPause = Instant.parse("2026-11-28T17:30:00Z"); // 23:00 IST (off-hours)
        Instant initialDeadline = tPause.plus(Duration.ofMinutes(60));
        saved.setSlaDeadline(initialDeadline);

        slaService.pauseOffHoursTicketsAt(tPause, LocalTime.of(23, 0));
        assertThat(saved.getSlaPaused()).isTrue();

        // Resume at 08:00 IST (+9h paused) -> previously broken! Must unpause & shift deadline +540m
        Instant tResume = tPause.plus(Duration.ofHours(9));
        slaService.pauseOffHoursTicketsAt(tResume, LocalTime.of(8, 0));
        assertThat(saved.getSlaPaused()).isFalse();
        assertThat(saved.getSlaDeadline()).isEqualTo(initialDeadline.plus(Duration.ofHours(9)));

        // Breach after resumed deadline triggers escalation level 1 -> level 2
        Instant afterBreach = saved.getSlaDeadline().plus(Duration.ofMinutes(5));
        var sweep1 = slaService.runEscalationSweepAt(afterBreach);
        assertThat(sweep1.escalations()).hasSize(1);
        assertThat(saved.getEscalationLevel()).isEqualTo(1);
    }
}
