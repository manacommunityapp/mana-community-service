package com.manacommunity.api.clubhouse.service;

import com.manacommunity.api.clubhouse.dto.ClubhouseDtos.*;
import com.manacommunity.api.clubhouse.entity.*;
import com.manacommunity.api.clubhouse.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class GuestPassTurnstileServiceTest {

    private GuestPassTurnstileService createServiceWithStores(
            Map<FacilityMonthlyQuotaId, FacilityMonthlyQuotaEntity> quotas,
            Set<LocalDate> holidays,
            Map<UUID, GuestAccessPassEntity> passes) {

        FacilityMonthlyQuotaRepository quotaRepo = Mockito.mock(FacilityMonthlyQuotaRepository.class);
        GuestPassPricingRuleRepository ruleRepo = Mockito.mock(GuestPassPricingRuleRepository.class);
        SocietyHolidayRepository holidayRepo = Mockito.mock(SocietyHolidayRepository.class);
        GuestAccessPassRepository passRepo = Mockito.mock(GuestAccessPassRepository.class);

        when(quotaRepo.findById(any())).thenAnswer(inv -> Optional.ofNullable(quotas.get(inv.getArgument(0))));
        when(quotaRepo.save(any())).thenAnswer(inv -> {
            FacilityMonthlyQuotaEntity q = inv.getArgument(0);
            quotas.put(q.getId(), q);
            return q;
        });

        when(ruleRepo.findBySocietyIdAndFacilityCodeAndDayCategory(anyLong(), anyString(), anyString()))
                .thenReturn(List.of());

        when(holidayRepo.existsById(any())).thenAnswer(inv -> {
            SocietyHolidayId id = inv.getArgument(0);
            return holidays.contains(id.getHolidayDate());
        });

        when(passRepo.save(any())).thenAnswer(inv -> {
            GuestAccessPassEntity p = inv.getArgument(0);
            passes.put(p.getPassId(), p);
            return p;
        });
        when(passRepo.findById(any())).thenAnswer(inv -> Optional.ofNullable(passes.get(inv.getArgument(0))));
        when(passRepo.findByJti(anyString())).thenAnswer(inv -> {
            String jti = inv.getArgument(0);
            return passes.values().stream().filter(p -> jti.equals(p.getJti())).findFirst();
        });
        when(passRepo.findBySocietyIdAndFlatIdAndBillingMonthOrderByCreatedAtDesc(anyLong(), anyLong(), anyString()))
                .thenAnswer(inv -> passes.values().stream().toList());
        when(passRepo.findBySocietyIdAndFacilityCodeAndStatusOrderByConsumedAtDesc(anyLong(), anyString(), anyString()))
                .thenAnswer(inv -> {
                    String status = inv.getArgument(2);
                    return passes.values().stream().filter(p -> status.equals(p.getStatus())).toList();
                });

        return new GuestPassTurnstileService(quotaRepo, ruleRepo, holidayRepo, passRepo);
    }

    @Test
    void appliesTwoFreeMonthlySlotsThenWeekdayAndWeekendDynamicPricing() {
        Map<FacilityMonthlyQuotaId, FacilityMonthlyQuotaEntity> quotas = new HashMap<>();
        Set<LocalDate> holidays = new HashSet<>();
        Map<UUID, GuestAccessPassEntity> passes = new LinkedHashMap<>();
        GuestPassTurnstileService service = createServiceWithStores(quotas, holidays, passes);

        LocalDate today = LocalDate.now(GuestPassTurnstileService.IST);
        // Pick deterministic same-month weekday & weekend dates for pricing/quota verification
        LocalDate sameMonthWeekday = today.withDayOfMonth(1);
        while (sameMonthWeekday.getDayOfWeek() == DayOfWeek.SATURDAY || sameMonthWeekday.getDayOfWeek() == DayOfWeek.SUNDAY) {
            sameMonthWeekday = sameMonthWeekday.plusDays(1);
        }
        LocalDate sameMonthWeekend = today.withDayOfMonth(1);
        while (sameMonthWeekend.getDayOfWeek() != DayOfWeek.SATURDAY) {
            sameMonthWeekend = sameMonthWeekend.plusDays(1);
        }

        // Quote 3 guests on weekday -> 2 free (₹0) + 1 paid @ ₹150
        GuestPassQuoteResponse weekdayQuote = service.quoteGuestPasses(
                new GuestPassQuoteRequest(1L, "SWIMMING_POOL", 1204L, sameMonthWeekday.toString(), 3));
        assertEquals("WEEKDAY", weekdayQuote.dayCategory());
        assertEquals(2, weekdayQuote.freeSlotsAllocated());
        assertEquals(1, weekdayQuote.paidSlotsRequired());
        assertEquals(15_000L, weekdayQuote.totalPayablePaise());

        // Issue 2 free guests for TODAY so all 2 monthly free slots are reserved in the same billing month
        IssueGuestPassesResponse issuedToday = service.issueGuestPasses(
                new IssueGuestPassesRequest(1L, "SWIMMING_POOL", 1204L, "Tower A - 1204", today.toString(),
                        List.of("Anita", "Vikram"), "9876543210", "FREE_QUOTA"));
        assertEquals(2, issuedToday.passes().size());
        assertEquals(0L, issuedToday.quote().totalPayablePaise());

        // Now all 2 free slots are reserved -> Weekend quote in same month for 1 guest must be ₹300
        GuestPassQuoteResponse weekendQuote = service.quoteGuestPasses(
                new GuestPassQuoteRequest(1L, "SWIMMING_POOL", 1204L, sameMonthWeekend.toString(), 1));
        assertEquals("WEEKEND", weekendQuote.dayCategory());
        assertEquals(0, weekendQuote.freeSlotsAllocated());
        assertEquals(30_000L, weekendQuote.totalPayablePaise());

        // Cancel Anita's unused free pass -> releases 1 free slot back!
        service.cancelGuestPass(issuedToday.passes().get(0).passId());
        GuestPassQuoteResponse afterCancelQuote = service.quoteGuestPasses(
                new GuestPassQuoteRequest(1L, "SWIMMING_POOL", 1204L, sameMonthWeekend.toString(), 2));
        assertEquals(1, afterCancelQuote.freeSlotsAllocated());
        assertEquals(30_000L, afterCancelQuote.totalPayablePaise());
    }

    @Test
    void validatesSignedQrPromotesQuotaAndBlocksAntiPassbackReplay() {
        Map<FacilityMonthlyQuotaId, FacilityMonthlyQuotaEntity> quotas = new HashMap<>();
        Set<LocalDate> holidays = new HashSet<>();
        Map<UUID, GuestAccessPassEntity> passes = new LinkedHashMap<>();
        GuestPassTurnstileService service = createServiceWithStores(quotas, holidays, passes);

        LocalDate today = LocalDate.now(GuestPassTurnstileService.IST);
        IssueGuestPassesResponse issued = service.issueGuestPasses(
                new IssueGuestPassesRequest(1L, "SWIMMING_POOL", 1204L, "Tower A - 1204", today.toString(),
                        List.of("Rohan"), "9988776655", "FREE_QUOTA"));
        GuestAccessPassDto pass = issued.passes().get(0);

        // Tampered token rejected
        TurnstileValidateResponse tampered = service.validateTurnstileScan(
                new TurnstileValidateRequest(1L, "SWIMMING_POOL", "POOL-GATE-1", pass.qrToken() + "tamper"));
        assertFalse(tampered.accessGranted());
        assertEquals("INVALID_SIGNATURE", tampered.reasonCode());

        // Valid first scan admits guest, increments occupancy, promotes free_reserved -> free_consumed
        TurnstileValidateResponse ok = service.validateTurnstileScan(
                new TurnstileValidateRequest(1L, "SWIMMING_POOL", "POOL-GATE-1", pass.qrToken()));
        assertTrue(ok.accessGranted());
        assertEquals(1, ok.poolOccupancy());

        FacilityMonthlyQuotaEntity quota = quotas.values().iterator().next();
        assertEquals((short) 0, quota.getFreeReserved());
        assertEquals((short) 1, quota.getFreeConsumed());

        // Immediate screenshot replay on Turnstile #2 blocked
        TurnstileValidateResponse replay = service.validateTurnstileScan(
                new TurnstileValidateRequest(1L, "SWIMMING_POOL", "POOL-GATE-2", pass.qrToken()));
        assertFalse(replay.accessGranted());
        assertEquals("ALREADY_CONSUMED", replay.reasonCode());
    }
}
