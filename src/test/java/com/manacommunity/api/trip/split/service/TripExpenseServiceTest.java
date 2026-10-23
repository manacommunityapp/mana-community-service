package com.manacommunity.api.trip.split.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.ExpenseRequest;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.ExpenseView;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.ParticipantInput;
import com.manacommunity.api.trip.split.entity.TripExpense;
import com.manacommunity.api.trip.split.repository.TripExpenseParticipantRepository;
import com.manacommunity.api.trip.split.repository.TripExpenseRepository;
import com.manacommunity.api.trip.split.repository.TripExpenseSplitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripExpenseServiceTest {

    @Mock
    private TripSplitAccess access;
    @Mock
    private TripExpenseRepository expenseRepository;
    @Mock
    private TripExpenseParticipantRepository participantRepository;
    @Mock
    private TripExpenseSplitRepository splitRepository;
    @Mock
    private TripMyMoneySync myMoney;

    @InjectMocks
    private TripExpenseService expenseService;

    private Trip testTrip;

    @BeforeEach
    void setUp() {
        testTrip = Trip.builder()
                .id("TRP-101")
                .title("Goa Trip")
                .organizerUserId(1L)
                .build();
    }

    @Test
    @DisplayName("Create expense by organizer on behalf of another participant succeeds")
    void testCreateExpenseByOrganizer() {
        when(access.requireTrip("TRP-101")).thenReturn(testTrip);
        when(access.participantIds(testTrip)).thenReturn(Set.of(1L, 2L, 3L));
        when(access.isOrganizer(testTrip, 1L)).thenReturn(true);
        when(access.names(anySet())).thenReturn(Map.of(1L, "Organizer", 2L, "Ravi", 3L, "Priya"));

        when(expenseRepository.save(any(TripExpense.class))).thenAnswer(inv -> {
            TripExpense e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        ExpenseRequest req = new ExpenseRequest(
                "FOOD",
                "Dinner",
                BigDecimal.valueOf(1500),
                2L, // paid by Ravi
                LocalDate.now(),
                "INR",
                null,
                "EQUAL",
                List.of(
                        new ParticipantInput(1L, null, null, null, null),
                        new ParticipantInput(2L, null, null, null, null),
                        new ParticipantInput(3L, null, null, null, null)
                )
        );

        ExpenseView view = expenseService.create("TRP-101", req, 1L);

        assertNotNull(view);
        assertEquals(10L, view.id());
        assertEquals("ACTIVE", view.status());
        verify(expenseRepository, atLeastOnce()).save(any(TripExpense.class));
        verify(myMoney).syncExpense(eq(testTrip), any(TripExpense.class));
    }

    @Test
    @DisplayName("Non-organizer cannot record expense paid by someone else")
    void testNonOrganizerCannotRecordOtherPayer() {
        when(access.requireTrip("TRP-101")).thenReturn(testTrip);
        when(access.participantIds(testTrip)).thenReturn(Set.of(1L, 2L, 3L));
        when(access.isOrganizer(testTrip, 2L)).thenReturn(false);

        ExpenseRequest req = new ExpenseRequest(
                "FOOD",
                "Dinner",
                BigDecimal.valueOf(1500),
                1L, // Ravi trying to record that Sandeep paid
                LocalDate.now(),
                "INR",
                null,
                "EQUAL",
                List.of(new ParticipantInput(2L, null, null, null, null))
        );

        assertThrows(UnauthorizedActionException.class, () ->
                expenseService.create("TRP-101", req, 2L));
    }

    @Test
    @DisplayName("Non-payer and non-organizer cannot void an expense")
    void testNonPayerCannotVoidExpense() {
        when(access.requireTrip("TRP-101")).thenReturn(testTrip);
        when(access.participantIds(testTrip)).thenReturn(Set.of(1L, 2L, 3L));
        when(access.isOrganizer(testTrip, 3L)).thenReturn(false);

        TripExpense existing = TripExpense.builder()
                .id(10L)
                .tripId("TRP-101")
                .paidByUserId(2L) // Ravi paid
                .status("ACTIVE")
                .build();
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(existing));

        // User 3 (Priya) tries to void
        assertThrows(UnauthorizedActionException.class, () ->
                expenseService.voidExpense("TRP-101", 10L, 3L));
    }

    @Test
    @DisplayName("Voiding an expense sets status to VOIDED and triggers myMoney sync")
    void testVoidExpenseSucceeds() {
        when(access.requireTrip("TRP-101")).thenReturn(testTrip);
        when(access.participantIds(testTrip)).thenReturn(Set.of(1L, 2L, 3L));
        when(access.isOrganizer(testTrip, 1L)).thenReturn(true);
        when(access.names(anySet())).thenReturn(Map.of(2L, "Ravi"));

        TripExpense existing = TripExpense.builder()
                .id(10L)
                .tripId("TRP-101")
                .paidByUserId(2L)
                .status("ACTIVE")
                .categoryCode("FOOD")
                .description("Dinner")
                .expenseDate(LocalDate.now())
                .totalPaise(150000L)
                .build();
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(expenseRepository.save(any(TripExpense.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseView view = expenseService.voidExpense("TRP-101", 10L, 1L);

        assertEquals("VOIDED", view.status());
        verify(myMoney).syncExpense(eq(testTrip), eq(existing));
    }
}
