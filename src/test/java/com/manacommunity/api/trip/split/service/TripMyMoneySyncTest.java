package com.manacommunity.api.trip.split.service;

import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.split.entity.MyMoneyMode;
import com.manacommunity.api.trip.split.entity.TripExpense;
import com.manacommunity.api.trip.split.entity.TripExpenseSplit;
import com.manacommunity.api.trip.split.entity.TripMyMoneyPref;
import com.manacommunity.api.trip.split.port.TripMyMoneyPort;
import com.manacommunity.api.trip.split.repository.TripExpenseRepository;
import com.manacommunity.api.trip.split.repository.TripExpenseSplitRepository;
import com.manacommunity.api.trip.split.repository.TripMyMoneyPrefRepository;
import com.manacommunity.api.trip.split.repository.TripSettlementPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripMyMoneySyncTest {

    @Mock
    private TripMyMoneyPrefRepository prefRepository;
    @Mock
    private TripExpenseRepository expenseRepository;
    @Mock
    private TripExpenseSplitRepository splitRepository;
    @Mock
    private TripSettlementPaymentRepository paymentRepository;
    @Mock
    private TripMyMoneyPort port;

    @InjectMocks
    private TripMyMoneySync sync;

    private Trip trip;

    @BeforeEach
    void setUp() {
        trip = Trip.builder()
                .id("TRP-101")
                .title("Goa Trip")
                .build();
    }

    @Test
    @DisplayName("No sync occurs if users have no preference or mode is OFF")
    void testNoSyncWhenOff() {
        when(prefRepository.findByTripId("TRP-101")).thenReturn(List.of());

        TripExpense expense = TripExpense.builder()
                .id(1L)
                .tripId("TRP-101")
                .status(TripExpense.ACTIVE)
                .build();

        sync.syncExpense(trip, expense);

        verifyNoInteractions(port);
    }

    @Test
    @DisplayName("Syncs user share to My Money when user has SHARE preference")
    void testSyncShareWhenModeIsShare() {
        TripMyMoneyPref pref = TripMyMoneyPref.builder()
                .tripId("TRP-101")
                .userId(2L)
                .mode(MyMoneyMode.SHARE)
                .build();
        when(prefRepository.findByTripId("TRP-101")).thenReturn(List.of(pref));

        TripExpense expense = TripExpense.builder()
                .id(1L)
                .tripId("TRP-101")
                .status(TripExpense.ACTIVE)
                .description("Beach Resort")
                .expenseDate(LocalDate.now())
                .build();

        TripExpenseSplit split = TripExpenseSplit.builder()
                .expenseId(1L)
                .userId(2L)
                .sharePaise(400000L) // 4000 rupees
                .build();
        when(splitRepository.findByExpenseId(1L)).thenReturn(List.of(split));

        sync.syncExpense(trip, expense);

        verify(port).upsert(
                eq(2L),
                eq("TRIP_SPLIT_SHARE"),
                eq("expense-1"),
                eq(false),
                eq(400000L),
                eq(expense.getExpenseDate()),
                contains("Beach Resort")
        );
    }

    @Test
    @DisplayName("Removes projected transaction when expense is voided")
    void testVoidRemovesProjection() {
        TripMyMoneyPref pref = TripMyMoneyPref.builder()
                .tripId("TRP-101")
                .userId(2L)
                .mode(MyMoneyMode.SHARE)
                .build();
        when(prefRepository.findByTripId("TRP-101")).thenReturn(List.of(pref));

        TripExpense expense = TripExpense.builder()
                .id(1L)
                .tripId("TRP-101")
                .status(TripExpense.VOIDED)
                .description("Cancelled Event")
                .expenseDate(LocalDate.now())
                .build();

        TripExpenseSplit split = TripExpenseSplit.builder()
                .expenseId(1L)
                .userId(2L)
                .sharePaise(200000L)
                .build();
        when(splitRepository.findByExpenseId(1L)).thenReturn(List.of(split));

        sync.syncExpense(trip, expense);

        verify(port).remove(eq(2L), eq("TRIP_SPLIT_SHARE"), eq("expense-1"));
        verify(port, never()).upsert(any(), any(), any(), anyBoolean(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Changing preference clears previous trip entries before applying new mode")
    void testApplyPreferenceClearsOldDataFirst() {
        TripExpense expense = TripExpense.builder().id(10L).tripId("TRP-101").build();
        when(expenseRepository.findByTripIdOrderByExpenseDateDescIdDesc("TRP-101")).thenReturn(List.of(expense));
        when(paymentRepository.findByTripIdOrderByCreatedAtDesc("TRP-101")).thenReturn(List.of());

        sync.applyPreference(trip, 5L, MyMoneyMode.OFF);

        verify(port).remove(eq(5L), eq("TRIP_SPLIT_SHARE"), eq("expense-10"));
        verify(port, never()).upsert(any(), any(), any(), anyBoolean(), anyLong(), any(), any());
    }
}
