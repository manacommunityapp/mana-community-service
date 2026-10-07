package com.manacommunity.api.trip.split.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.BudgetRequest;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.DashboardView;
import com.manacommunity.api.trip.split.engine.BalanceCalculator.Balance;
import com.manacommunity.api.trip.split.entity.TripBudget;
import com.manacommunity.api.trip.split.entity.TripExpense;
import com.manacommunity.api.trip.split.repository.TripBudgetRepository;
import com.manacommunity.api.trip.split.repository.TripExpenseRepository;
import com.manacommunity.api.trip.split.support.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Trip budget versus spend, plus the caller's own share/paid/receive position. */
@Service
@RequiredArgsConstructor
public class TripBudgetService {

    private final TripSplitAccess access;
    private final TripBudgetRepository budgetRepository;
    private final TripExpenseRepository expenseRepository;
    private final TripSettlementService settlementService;

    @Transactional(readOnly = true)
    public DashboardView dashboard(String tripId, Long actorId) {
        Trip trip = access.requireTrip(tripId);
        Set<Long> members = access.participantIds(trip);
        access.requireMember(members, actorId);
        return build(trip, members, actorId);
    }

    @Transactional
    public DashboardView setBudget(String tripId, BudgetRequest req, Long actorId) {
        if (req == null) {
            throw new InvalidInputException("Budget details are required");
        }
        Trip trip = access.requireTrip(tripId);
        Set<Long> members = access.participantIds(trip);
        access.requireMember(members, actorId);
        if (!access.isOrganizer(trip, actorId)) {
            throw new UnauthorizedActionException("only the trip organizer can set the budget");
        }

        long estimated = Money.toPaise(req.estimatedAmount());
        if (estimated < 0) {
            throw new InvalidInputException("Budget cannot be negative");
        }
        String currency = req.currency() == null || req.currency().isBlank()
                ? "INR" : req.currency().trim().toUpperCase();
        if (currency.length() > 10) {
            throw new InvalidInputException("Currency code is too long");
        }

        TripBudget budget = budgetRepository.findById(tripId)
                .orElseGet(() -> TripBudget.builder().tripId(tripId).build());
        budget.setEstimatedPaise(estimated);
        budget.setCurrency(currency);
        budgetRepository.save(budget);

        return build(trip, members, actorId);
    }

    private DashboardView build(Trip trip, Set<Long> members, Long actorId) {
        long spent = 0;
        long unsplit = 0;
        Map<String, Long> byCategory = new TreeMap<>();
        for (TripExpense e : expenseRepository.findByTripIdAndStatusIn(trip.getId(),
                java.util.List.of(TripExpense.ACTIVE, TripExpense.UNSPLIT))) {
            spent += e.getTotalPaise();
            byCategory.merge(e.getCategoryCode(), e.getTotalPaise(), Long::sum);
            if (TripExpense.UNSPLIT.equals(e.getStatus())) {
                unsplit += e.getTotalPaise();
            }
        }

        Balance mine = settlementService.computeBalances(trip, members).get(actorId);
        long net = mine.netPaise();

        BigDecimal estimated = null;
        BigDecimal remaining = null;
        TripBudget budget = budgetRepository.findById(trip.getId()).orElse(null);
        if (budget != null) {
            estimated = Money.toRupees(budget.getEstimatedPaise());
            remaining = Money.toRupees(budget.getEstimatedPaise() - spent);
        }

        Map<String, BigDecimal> categories = new TreeMap<>();
        byCategory.forEach((code, paise) -> categories.put(code, Money.toRupees(paise)));

        return new DashboardView(estimated, Money.toRupees(spent), remaining, Money.toRupees(unsplit),
                Money.toRupees(mine.sharePaise()), Money.toRupees(mine.paidPaise()),
                Money.toRupees(Math.max(net, 0)), Money.toRupees(Math.max(-net, 0)), categories);
    }
}
