package com.manacommunity.api.trip.split.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.ExpenseRequest;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.ExpenseView;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.ParticipantInput;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.ShareView;
import com.manacommunity.api.trip.split.engine.SplitCalculator;
import com.manacommunity.api.trip.split.engine.SplitInput;
import com.manacommunity.api.trip.split.engine.SplitMethod;
import com.manacommunity.api.trip.split.entity.TripExpense;
import com.manacommunity.api.trip.split.entity.TripExpenseParticipant;
import com.manacommunity.api.trip.split.entity.TripExpenseSplit;
import com.manacommunity.api.trip.split.repository.TripExpenseParticipantRepository;
import com.manacommunity.api.trip.split.repository.TripExpenseRepository;
import com.manacommunity.api.trip.split.repository.TripExpenseSplitRepository;
import com.manacommunity.api.trip.split.support.Money;
import com.manacommunity.api.trip.split.support.TripExpenseCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TripExpenseService {

    private final TripSplitAccess access;
    private final TripExpenseRepository expenseRepository;
    private final TripExpenseParticipantRepository participantRepository;
    private final TripExpenseSplitRepository splitRepository;
    private final TripMyMoneySync myMoney;

    @Transactional
    public ExpenseView create(String tripId, ExpenseRequest req, Long actorId) {
        requireBody(req);
        Trip trip = access.requireTrip(tripId);
        Set<Long> members = access.participantIds(trip);
        access.requireMember(members, actorId);

        Long payer = req.paidByUserId() != null ? req.paidByUserId() : actorId;
        if (!payer.equals(actorId) && !access.isOrganizer(trip, actorId)) {
            throw new UnauthorizedActionException("only the trip organizer can record an expense paid by someone else");
        }
        if (!members.contains(payer)) {
            throw new InvalidInputException("The person who paid is not a participant of this trip");
        }

        TripExpense expense = TripExpense.builder()
                .tripId(tripId)
                .paidByUserId(payer)
                .createdBy(actorId)
                .totalPaise(1) // replaced by applyFields; keeps the NOT NULL check happy until then
                .categoryCode(TripExpenseCategory.MISCELLANEOUS.name())
                .description("-")
                .expenseDate(LocalDate.now())
                .build();
        applyFields(expense, req);
        expense = expenseRepository.save(expense);
        applySplit(expense, req, members);
        expense = expenseRepository.save(expense);

        myMoney.syncExpense(trip, expense);
        return view(expense);
    }

    @Transactional
    public ExpenseView update(String tripId, Long expenseId, ExpenseRequest req, Long actorId) {
        requireBody(req);
        Trip trip = access.requireTrip(tripId);
        Set<Long> members = access.participantIds(trip);
        access.requireMember(members, actorId);

        TripExpense expense = requireExpense(tripId, expenseId);
        requireCanModify(trip, expense, actorId);
        if (TripExpense.VOIDED.equals(expense.getStatus())) {
            throw new InvalidInputException("A voided expense cannot be edited");
        }

        if (req.paidByUserId() != null && !req.paidByUserId().equals(expense.getPaidByUserId())) {
            if (!access.isOrganizer(trip, actorId)) {
                throw new UnauthorizedActionException("only the trip organizer can change who paid");
            }
            if (!members.contains(req.paidByUserId())) {
                throw new InvalidInputException("The person who paid is not a participant of this trip");
            }
            expense.setPaidByUserId(req.paidByUserId());
        }

        applyFields(expense, req);
        applySplit(expense, req, members);
        expense = expenseRepository.save(expense);

        myMoney.syncExpense(trip, expense);
        return view(expense);
    }

    @Transactional
    public ExpenseView voidExpense(String tripId, Long expenseId, Long actorId) {
        Trip trip = access.requireTrip(tripId);
        access.requireMember(access.participantIds(trip), actorId);

        TripExpense expense = requireExpense(tripId, expenseId);
        requireCanModify(trip, expense, actorId);

        expense.setStatus(TripExpense.VOIDED);
        expense = expenseRepository.save(expense);

        myMoney.syncExpense(trip, expense);
        return view(expense);
    }

    @Transactional(readOnly = true)
    public ExpenseView get(String tripId, Long expenseId, Long actorId) {
        Trip trip = access.requireTrip(tripId);
        access.requireMember(access.participantIds(trip), actorId);
        return view(requireExpense(tripId, expenseId));
    }

    @Transactional(readOnly = true)
    public List<ExpenseView> list(String tripId, Long actorId) {
        Trip trip = access.requireTrip(tripId);
        access.requireMember(access.participantIds(trip), actorId);

        List<TripExpense> expenses = expenseRepository.findByTripIdOrderByExpenseDateDescIdDesc(tripId);
        if (expenses.isEmpty()) {
            return List.of();
        }
        List<Long> ids = expenses.stream().map(TripExpense::getId).toList();
        Map<Long, Map<Long, Long>> sharesByExpense = new HashMap<>();
        for (TripExpenseSplit split : splitRepository.findByExpenseIdIn(ids)) {
            sharesByExpense.computeIfAbsent(split.getExpenseId(), k -> new LinkedHashMap<>())
                    .put(split.getUserId(), split.getSharePaise());
        }

        Set<Long> userIds = new HashSet<>();
        expenses.forEach(e -> userIds.add(e.getPaidByUserId()));
        sharesByExpense.values().forEach(m -> userIds.addAll(m.keySet()));
        Map<Long, String> names = access.names(userIds);

        List<ExpenseView> views = new ArrayList<>();
        for (TripExpense e : expenses) {
            views.add(toView(e, sharesByExpense.getOrDefault(e.getId(), Map.of()), names));
        }
        return views;
    }

    // ---- internals -------------------------------------------------------------------------------

    private void applyFields(TripExpense expense, ExpenseRequest req) {
        String description = req.description() == null ? "" : req.description().trim();
        if (description.isEmpty() || description.length() > 255) {
            throw new InvalidInputException("Description is required and must be at most 255 characters");
        }
        long total = Money.toPaise(req.totalAmount());
        if (total <= 0) {
            throw new InvalidInputException("Expense amount must be greater than zero");
        }
        String currency = req.currency() == null || req.currency().isBlank()
                ? "INR" : req.currency().trim().toUpperCase();
        if (currency.length() > 10) {
            throw new InvalidInputException("Currency code is too long");
        }

        expense.setCategoryCode(TripExpenseCategory.fromCode(req.categoryCode()).name());
        expense.setDescription(description);
        expense.setTotalPaise(total);
        expense.setCurrency(currency);
        expense.setExpenseDate(req.expenseDate() != null ? req.expenseDate() : LocalDate.now());
        expense.setReceiptUrl(req.receiptUrl());
    }

    /** Replaces the stored inputs and computed shares. With no split method the expense is left UNSPLIT. */
    private void applySplit(TripExpense expense, ExpenseRequest req, Set<Long> members) {
        // Inserts of IDENTITY entities run immediately, ahead of queued deletes, so flush the deletes first
        // or re-inserting the same (expense_id, user_id) trips the unique constraint.
        participantRepository.deleteByExpenseId(expense.getId());
        splitRepository.deleteByExpenseId(expense.getId());
        participantRepository.flush();

        if (req.splitMethod() == null || req.splitMethod().isBlank()) {
            expense.setStatus(TripExpense.UNSPLIT);
            expense.setSplitMethod(null);
            return;
        }

        SplitMethod method;
        try {
            method = SplitMethod.valueOf(req.splitMethod().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidInputException("Unknown split method: " + req.splitMethod());
        }
        if (req.participants() == null || req.participants().isEmpty()) {
            throw new InvalidInputException("Choose who shares this expense");
        }

        List<SplitInput> inputs = new ArrayList<>();
        List<TripExpenseParticipant> rows = new ArrayList<>();
        for (ParticipantInput p : req.participants()) {
            if (p.userId() == null || !members.contains(p.userId())) {
                throw new InvalidInputException("Everyone sharing an expense must be a participant of this trip");
            }
            long weight = p.weight() != null ? p.weight() : 1L;
            long quantity = p.quantity() != null ? p.quantity() : 1L;
            long basisPoints = method == SplitMethod.PERCENTAGE ? Money.percentToBasisPoints(p.percentage()) : 0L;
            long exact = method == SplitMethod.EXACT ? Money.toPaise(p.amount()) : 0L;

            inputs.add(new SplitInput(p.userId(), weight, quantity, basisPoints, exact));
            rows.add(TripExpenseParticipant.builder()
                    .expenseId(expense.getId()).userId(p.userId())
                    .weight(weight).quantity(quantity).percentageBp(basisPoints).exactPaise(exact)
                    .build());
        }

        Map<Long, Long> shares = SplitCalculator.calculate(method, expense.getTotalPaise(), inputs);

        participantRepository.saveAll(rows);
        List<TripExpenseSplit> splits = new ArrayList<>();
        shares.forEach((userId, share) -> splits.add(TripExpenseSplit.builder()
                .expenseId(expense.getId()).userId(userId).sharePaise(share).build()));
        splitRepository.saveAll(splits);

        expense.setStatus(TripExpense.ACTIVE);
        expense.setSplitMethod(method.name());
    }

    private TripExpense requireExpense(String tripId, Long expenseId) {
        TripExpense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip expense", expenseId));
        if (!expense.getTripId().equals(tripId)) {
            // Same answer as a missing expense, so ids from other trips are not revealed.
            throw new ResourceNotFoundException("Trip expense", expenseId);
        }
        return expense;
    }

    private void requireCanModify(Trip trip, TripExpense expense, Long actorId) {
        boolean isPayer = actorId.equals(expense.getPaidByUserId());
        if (!isPayer && !access.isOrganizer(trip, actorId)) {
            throw new UnauthorizedActionException("only the person who paid or the trip organizer can change this expense");
        }
    }

    private static void requireBody(ExpenseRequest req) {
        if (req == null) {
            throw new InvalidInputException("Expense details are required");
        }
    }

    private ExpenseView view(TripExpense expense) {
        Map<Long, Long> shares = new LinkedHashMap<>();
        for (TripExpenseSplit split : splitRepository.findByExpenseId(expense.getId())) {
            shares.put(split.getUserId(), split.getSharePaise());
        }
        Set<Long> userIds = new HashSet<>(shares.keySet());
        userIds.add(expense.getPaidByUserId());
        return toView(expense, shares, access.names(userIds));
    }

    private ExpenseView toView(TripExpense e, Map<Long, Long> shares, Map<Long, String> names) {
        List<ShareView> shareViews = new ArrayList<>();
        shares.forEach((userId, share) ->
                shareViews.add(new ShareView(userId, names.get(userId), Money.toRupees(share))));
        return new ExpenseView(e.getId(), e.getTripId(), e.getCategoryCode(), e.getDescription(),
                Money.toRupees(e.getTotalPaise()), e.getCurrency(), e.getPaidByUserId(),
                names.get(e.getPaidByUserId()), e.getExpenseDate(), e.getStatus(), e.getSplitMethod(),
                e.getReceiptUrl(), shareViews);
    }
}
