package com.manacommunity.api.unit.service;

import com.manacommunity.api.homeservices.entity.*;
import com.manacommunity.api.homeservices.repository.*;
import com.manacommunity.api.homeservices.service.MorningSubscriptionEngineService;
import com.manacommunity.api.homeservices.service.SlotContentionPricingService;
import com.manacommunity.api.homeservices.service.SplitSettlementLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeServicesSubscriptionAndSettlementTest {

    @Mock private HsRecurringPlanRepository planRepo;
    @Mock private MorningSubOverrideRepository overrideRepo;
    @Mock private MorningDeliveryInstanceRepository instanceRepo;

    @Mock private HsWorkerAvailabilityRepository availabilityRepo;
    @Mock private HsBookingRepository bookingRepo;

    @Mock private MultiTenderSettlementRepository settlementRepo;
    @Mock private SettlementLedgerEntryRepository ledgerRepo;

    private MorningSubscriptionEngineService morningEngine;
    private SlotContentionPricingService slotPricingService;
    private SplitSettlementLedgerService splitLedgerService;

    @BeforeEach
    void setUp() {
        morningEngine = new MorningSubscriptionEngineService(planRepo, overrideRepo, instanceRepo);
        slotPricingService = new SlotContentionPricingService(availabilityRepo, bookingRepo);
        splitLedgerService = new SplitSettlementLedgerService(settlementRepo, ledgerRepo);
    }

    @Test
    @DisplayName("Enforces 23:59 subscription cutoff, builds floor-sorted vendor manifest, and sweeps unmarked auto-refunds")
    void shouldFreezeMorningSubscriptionsBuildSortedManifestAndAutoRefund() {
        LocalDate tomorrow = LocalDate.of(2026, 11, 28);

        // 1. After 23:59 cutoff -> override rolls automatically to D+2 (2026-11-29)
        when(overrideRepo.findByPlanIdAndDeliveryDate(anyLong(), any(LocalDate.class))).thenReturn(Optional.empty());
        when(overrideRepo.save(any(MorningSubOverride.class))).thenAnswer(inv -> inv.getArgument(0));

        var ovRes = morningEngine.upsertOverride(1L, tomorrow, 3, false,
                LocalDateTime.of(2026, 11, 27, 23, 59, 30));
        assertThat(ovRes.rolledPastCutoff()).isTrue();
        assertThat(ovRes.effectiveDeliveryDate()).isEqualTo(tomorrow.plusDays(1));

        // 2. Freeze active plans into spatial vendor manifest + skip vacation + flag low wallet balance
        List<HsRecurringPlan> plans = List.of(
                HsRecurringPlan.builder().id(1L).communityId(10L).vendorId(5L).userId(101L)
                        .skuCode("MILK_1L").towerBlock("B").floorNumber(2).flatNumber("B-201")
                        .defaultQty(2).unitPrice(new BigDecimal("64.00")).status("ACTIVE")
                        .nextServiceDate(tomorrow).build(),
                HsRecurringPlan.builder().id(2L).communityId(10L).vendorId(5L).userId(102L)
                        .skuCode("MILK_1L").towerBlock("A").floorNumber(8).flatNumber("A-804")
                        .defaultQty(1).unitPrice(new BigDecimal("64.00")).status("ACTIVE")
                        .nextServiceDate(tomorrow).build(),
                HsRecurringPlan.builder().id(3L).communityId(10L).vendorId(5L).userId(103L)
                        .skuCode("BREAD_1").towerBlock("A").floorNumber(1).flatNumber("A-102")
                        .defaultQty(1).unitPrice(new BigDecimal("45.00")).status("ACTIVE")
                        .vacationFrom(tomorrow.minusDays(1)).vacationUntil(tomorrow.plusDays(3))
                        .nextServiceDate(tomorrow).build(),
                HsRecurringPlan.builder().id(4L).communityId(10L).vendorId(5L).userId(104L) // low wallet
                        .skuCode("MILK_1L").towerBlock("C").floorNumber(5).flatNumber("C-501")
                        .defaultQty(1).unitPrice(new BigDecimal("64.00")).status("ACTIVE")
                        .nextServiceDate(tomorrow).build()
        );

        List<MorningDeliveryInstance> savedInstances = new ArrayList<>();
        when(planRepo.findByStatusAndNextServiceDateLessThanEqual("ACTIVE", tomorrow)).thenReturn(plans);
        when(instanceRepo.findByPlanIdAndDeliveryDate(anyLong(), eq(tomorrow))).thenReturn(Optional.empty());
        when(instanceRepo.save(any(MorningDeliveryInstance.class))).thenAnswer(inv -> {
            MorningDeliveryInstance inst = inv.getArgument(0);
            inst.setId((long) (savedInstances.size() + 1));
            savedInstances.add(inst);
            return inst;
        });
        when(instanceRepo.findByVendorIdAndDeliveryDateOrderByCommunityIdAscTowerBlockAscFloorNumberDescFlatNumberAsc(5L, tomorrow))
                .thenReturn(savedInstances);

        var manifest = morningEngine.freezeAndBuildVendorManifest(5L, tomorrow, (uid, amt) -> uid != 104L);
        assertThat(manifest.totalFlats()).isEqualTo(2); // Plan 3 vacation excluded; Plan 4 low-balance skipped
        assertThat(manifest.lowBalancePausedCount()).isEqualTo(1);
        assertThat(manifest.skuPackingTotals().get("MILK_1L")).isEqualTo(3); // 2 + 1
        // Sorted Tower A floor 8 first, then Tower B floor 2
        assertThat(manifest.orderedStops().get(0).getFlatNumber()).isEqualTo("A-804");
        assertThat(manifest.orderedStops().get(1).getFlatNumber()).isEqualTo("B-201");

        // 3. Direct vendor mark OUT_OF_STOCK triggers instant AUTO_REFUNDED on item 2
        MorningDeliveryInstance secondStop = manifest.orderedStops().get(1);
        when(instanceRepo.findById(secondStop.getId())).thenReturn(Optional.of(secondStop));
        when(instanceRepo.save(any(MorningDeliveryInstance.class))).thenAnswer(inv -> inv.getArgument(0));
        MorningDeliveryInstance oosResult = morningEngine.settleDeliveryOutcome(
                secondStop.getId(), false, "OUT_OF_STOCK: Morning batch exhausted");
        assertThat(oosResult.getDeliveryStatus()).isEqualTo("OUT_OF_STOCK");
        assertThat(oosResult.getWalletStatus()).isEqualTo("AUTO_REFUNDED");

        // 4. Sweep remaining unmarked instance at 09:30 AM -> AUTO_REFUNDED
        when(instanceRepo.findByDeliveryDateAndDeliveryStatus(tomorrow, "FROZEN_FOR_DISPATCH"))
                .thenReturn(savedInstances.stream().filter(i -> "FROZEN_FOR_DISPATCH".equals(i.getDeliveryStatus())).toList());
        int refunded = morningEngine.sweepUnmarkedToAutoRefund(tomorrow);
        assertThat(refunded).isEqualTo(1);
        assertThat(savedInstances.get(0).getWalletStatus()).isEqualTo("AUTO_REFUNDED");
    }

    @Test
    @DisplayName("Applies dynamic surge/cluster pricing, travel buffer check, and rejects simultaneous double-booking")
    void shouldEnforceSlotContentionTravelBufferAndDynamicPricing() {
        // Dynamic pricing: base 1000 * 1.15 weekend * 1.30 peak = 1495.00 - 10% cluster = 1345.50
        var quote = slotPricingService.calculateDynamicPrice(new BigDecimal("1000.00"), true, 90, true);
        assertThat(quote.quotedPrice()).isEqualByComparingTo("1345.50");
        assertThat(quote.travelBufferMinutes()).isEqualTo(5);

        LocalDate today = LocalDate.of(2026, 11, 28);
        HsWorkerAvailability bookedSlotComm1 = HsWorkerAvailability.builder()
                .id(10L).workerId(77L).communityId(1L).availableDate(today)
                .slotStart(LocalTime.of(14, 0)).slotEnd(LocalTime.of(15, 0))
                .isBooked(true).travelBufferMinutes(25).build();
        HsWorkerAvailability targetSlotComm2 = HsWorkerAvailability.builder()
                .id(11L).workerId(77L).communityId(2L).availableDate(today)
                .slotStart(LocalTime.of(15, 10)).slotEnd(LocalTime.of(16, 10))
                .isBooked(false).build();

        when(availabilityRepo.findByWorkerIdAndAvailableDateGreaterThanEqual(77L, today))
                .thenReturn(List.of(bookedSlotComm1, targetSlotComm2));

        // 15:10 in Community 2 overlaps 14:00-15:00 + 25m transit buffer (15:25)!
        assertThat(slotPricingService.isSlotClearWithTravelBuffer(
                77L, 2L, today, LocalTime.of(15, 10), LocalTime.of(16, 10))).isFalse();

        // Hold & atomic booking guard
        HsWorkerAvailability cleanSlot = HsWorkerAvailability.builder()
                .id(12L).workerId(88L).communityId(1L).availableDate(today)
                .slotStart(LocalTime.of(16, 0)).slotEnd(LocalTime.of(17, 0))
                .isBooked(false).build();
        when(availabilityRepo.findById(12L)).thenReturn(Optional.of(cleanSlot));
        when(availabilityRepo.findByWorkerIdAndAvailableDateGreaterThanEqual(88L, today)).thenReturn(List.of(cleanSlot));
        when(availabilityRepo.save(any(HsWorkerAvailability.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepo.save(any(HsBooking.class))).thenAnswer(inv -> {
            HsBooking b = inv.getArgument(0);
            b.setId(501L);
            return b;
        });

        slotPricingService.acquireCheckoutHold(12L, "HOLD-RESIDENT-1", Instant.now());

        // Rival resident with different token blocked while hold active
        assertThatThrownBy(() -> slotPricingService.acquireCheckoutHold(12L, "HOLD-RESIDENT-2", Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("409 SLOT_HELD");

        HsBooking confirmed = slotPricingService.confirmHeldSlotBooking(
                12L, "HOLD-RESIDENT-1", 1L, 200L, "A-402", "AC_REPAIR", quote.quotedPrice(), Instant.now());
        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(cleanSlot.getIsBooked()).isTrue();

        // Simultaneous second commit rejected via 409 SLOT_CONFLICT
        assertThatThrownBy(() -> slotPricingService.confirmHeldSlotBooking(
                12L, "HOLD-RESIDENT-1", 1L, 201L, "B-303", "AC_REPAIR", quote.quotedPrice(), Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("409 SLOT_CONFLICT");
    }

    @Test
    @DisplayName("Reconciles ₹1,200 multi-tender checkout into balanced double-entry ledger and compensates on UPI timeout")
    void shouldBalanceSplitSettlementLedgerAndCompensateUpiFailure() {
        List<SettlementLedgerEntry> ledgerStore = new ArrayList<>();
        Map<String, MultiTenderSettlement> settlementMap = new HashMap<>();

        when(settlementRepo.save(any(MultiTenderSettlement.class))).thenAnswer(inv -> {
            MultiTenderSettlement s = inv.getArgument(0);
            if (s.getId() == null) s.setId(900L);
            settlementMap.put(s.getOrderRef(), s);
            return s;
        });
        when(settlementRepo.findByOrderRef(anyString())).thenAnswer(inv -> Optional.ofNullable(settlementMap.get(inv.getArgument(0))));
        when(ledgerRepo.save(any(SettlementLedgerEntry.class))).thenAnswer(inv -> {
            SettlementLedgerEntry e = inv.getArgument(0);
            e.setId((long) (ledgerStore.size() + 1));
            ledgerStore.add(e);
            return e;
        });
        when(ledgerRepo.findBySettlementIdOrderByIdAsc(900L)).thenAnswer(inv -> ledgerStore);

        // ₹1,200 = ₹200 Reward Points + ₹100 Brand Coupon + ₹900 UPI
        var res = splitLedgerService.initiateAndReserve(SplitSettlementLedgerService.MultiTenderQuoteRequest.builder()
                .orderRef("ORD-SPLIT-1200")
                .bookingId(501L).communityId(1L).vendorId(77L).userId(200L)
                .grossAmount(new BigDecimal("1200.00"))
                .pointsUsed(new BigDecimal("200.00"))
                .couponCode("DAIKIN100")
                .couponDiscount(new BigDecimal("100.00"))
                .upiAmount(new BigDecimal("900.00"))
                .build());

        assertThat(res.balanced()).isTrue();
        assertThat(res.settlement().getPlatformCommission()).isEqualByComparingTo("180.00");
        assertThat(res.settlement().getCommissionGst()).isEqualByComparingTo("32.40");
        assertThat(res.settlement().getVendorNetPayout()).isEqualByComparingTo("987.60");
        assertThat(res.entries()).hasSize(6); // 3 DEBIT funding legs + 3 CREDIT allocation legs

        // Simulate UPI timeout -> reverse balanced compensating entries & restore points/coupon
        var comp = splitLedgerService.compensateOnUpiFailure("ORD-SPLIT-1200", "RAZORPAY_TIMEOUT");
        assertThat(comp.settlement().getState()).isEqualTo("COMPENSATED");
        assertThat(comp.balanced()).isTrue();
        assertThat(comp.entries()).hasSize(12);
    }
}
