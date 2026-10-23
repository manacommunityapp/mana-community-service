package com.manacommunity.api.trip.split.service;

import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.split.entity.MyMoneyMode;
import com.manacommunity.api.trip.split.entity.TripExpense;
import com.manacommunity.api.trip.split.entity.TripExpenseSplit;
import com.manacommunity.api.trip.split.entity.TripMyMoneyPref;
import com.manacommunity.api.trip.split.entity.TripSettlementPayment;
import com.manacommunity.api.trip.split.port.TripMyMoneyPort;
import com.manacommunity.api.trip.split.repository.TripExpenseRepository;
import com.manacommunity.api.trip.split.repository.TripExpenseSplitRepository;
import com.manacommunity.api.trip.split.repository.TripMyMoneyPrefRepository;
import com.manacommunity.api.trip.split.repository.TripSettlementPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps My Money in step with a trip, but only for users who opted in. Trip data never flows to My Money
 * on its own, and a failure writing to My Money is logged without failing the trip operation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TripMyMoneySync {

    static final String SHARE = "TRIP_SPLIT_SHARE";
    static final String SENT = "TRIP_SETTLEMENT_SENT";
    static final String RECEIVED = "TRIP_SETTLEMENT_RECEIVED";

    private static final int LABEL_LIMIT = 250;

    private final TripMyMoneyPrefRepository prefRepository;
    private final TripExpenseRepository expenseRepository;
    private final TripExpenseSplitRepository splitRepository;
    private final TripSettlementPaymentRepository paymentRepository;
    private final TripMyMoneyPort port;

    public MyMoneyMode modeOf(String tripId, Long userId) {
        return prefRepository.findByTripIdAndUserId(tripId, userId)
                .map(TripMyMoneyPref::getMode)
                .orElse(MyMoneyMode.OFF);
    }

    /** Call after an expense is created, edited or voided. */
    public void syncExpense(Trip trip, TripExpense expense) {
        List<TripMyMoneyPref> prefs = prefRepository.findByTripId(trip.getId());
        if (prefs.stream().noneMatch(p -> p.getMode() == MyMoneyMode.SHARE)) {
            return;
        }
        Map<Long, Long> shares = sharesOf(expense.getId());
        for (TripMyMoneyPref pref : prefs) {
            if (pref.getMode() == MyMoneyMode.SHARE) {
                safely(() -> applyShare(trip, expense, pref.getUserId(), shares));
            }
        }
    }

    /** Call after a settlement payment is confirmed. */
    public void syncPayment(Trip trip, TripSettlementPayment payment) {
        if (!TripSettlementPayment.CONFIRMED.equals(payment.getStatus())) {
            return;
        }
        if (modeOf(trip.getId(), payment.getFromUserId()) == MyMoneyMode.SETTLEMENTS) {
            safely(() -> applyPayment(trip, payment, payment.getFromUserId(), false));
        }
        if (modeOf(trip.getId(), payment.getToUserId()) == MyMoneyMode.SETTLEMENTS) {
            safely(() -> applyPayment(trip, payment, payment.getToUserId(), true));
        }
    }

    /**
     * Call when a user changes their mode. Clears everything previously written for them on this trip,
     * then writes what the new mode calls for, so switching modes never leaves double-counted rows.
     */
    public void applyPreference(Trip trip, Long userId, MyMoneyMode mode) {
        List<TripExpense> expenses = expenseRepository.findByTripIdOrderByExpenseDateDescIdDesc(trip.getId());
        List<TripSettlementPayment> payments = paymentRepository.findByTripIdOrderByCreatedAtDesc(trip.getId());

        for (TripExpense e : expenses) {
            safely(() -> port.remove(userId, SHARE, expenseSource(e)));
        }
        for (TripSettlementPayment p : payments) {
            safely(() -> port.remove(userId, SENT, paymentSource(p)));
            safely(() -> port.remove(userId, RECEIVED, paymentSource(p)));
        }

        if (mode == MyMoneyMode.SHARE) {
            for (TripExpense e : expenses) {
                Map<Long, Long> shares = sharesOf(e.getId());
                safely(() -> applyShare(trip, e, userId, shares));
            }
        } else if (mode == MyMoneyMode.SETTLEMENTS) {
            for (TripSettlementPayment p : payments) {
                if (!TripSettlementPayment.CONFIRMED.equals(p.getStatus())) {
                    continue;
                }
                if (userId.equals(p.getFromUserId())) {
                    safely(() -> applyPayment(trip, p, userId, false));
                } else if (userId.equals(p.getToUserId())) {
                    safely(() -> applyPayment(trip, p, userId, true));
                }
            }
        }
    }

    private void applyShare(Trip trip, TripExpense expense, Long userId, Map<Long, Long> shares) {
        long share = TripExpense.ACTIVE.equals(expense.getStatus()) ? shares.getOrDefault(userId, 0L) : 0L;
        if (share > 0) {
            port.upsert(userId, SHARE, expenseSource(expense), false, share, expense.getExpenseDate(),
                    label(trip.getTitle() + ": " + expense.getDescription()));
        } else {
            port.remove(userId, SHARE, expenseSource(expense));
        }
    }

    private void applyPayment(Trip trip, TripSettlementPayment payment, Long userId, boolean income) {
        LocalDate date = payment.getConfirmedAt() != null ? payment.getConfirmedAt().toLocalDate() : LocalDate.now();
        String text = income ? "Trip settlement received: " : "Trip settlement paid: ";
        port.upsert(userId, income ? RECEIVED : SENT, paymentSource(payment), income, payment.getAmountPaise(),
                date, label(text + trip.getTitle()));
    }

    private Map<Long, Long> sharesOf(Long expenseId) {
        Map<Long, Long> shares = new HashMap<>();
        for (TripExpenseSplit split : splitRepository.findByExpenseId(expenseId)) {
            shares.put(split.getUserId(), split.getSharePaise());
        }
        return shares;
    }

    private static String expenseSource(TripExpense e) {
        return "expense-" + e.getId();
    }

    private static String paymentSource(TripSettlementPayment p) {
        return "payment-" + p.getId();
    }

    private static String label(String text) {
        return text.length() <= LABEL_LIMIT ? text : text.substring(0, LABEL_LIMIT);
    }

    private void safely(Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            log.warn("[TripSplit] My Money sync failed, trip data is unaffected: {}", ex.getMessage());
        }
    }
}
