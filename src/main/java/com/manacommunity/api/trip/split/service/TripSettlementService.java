package com.manacommunity.api.trip.split.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.BalanceView;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.PaymentRequest;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.PaymentView;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.SummaryView;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.TransferView;
import com.manacommunity.api.trip.split.engine.BalanceCalculator;
import com.manacommunity.api.trip.split.engine.BalanceCalculator.Balance;
import com.manacommunity.api.trip.split.engine.BalanceCalculator.ExpenseLine;
import com.manacommunity.api.trip.split.engine.BalanceCalculator.PaymentLine;
import com.manacommunity.api.trip.split.engine.SettlementOptimizer;
import com.manacommunity.api.trip.split.engine.SettlementOptimizer.Transfer;
import com.manacommunity.api.trip.split.entity.TripExpense;
import com.manacommunity.api.trip.split.entity.TripExpenseSplit;
import com.manacommunity.api.trip.split.entity.TripSettlementPayment;
import com.manacommunity.api.trip.split.repository.TripExpenseRepository;
import com.manacommunity.api.trip.split.repository.TripExpenseSplitRepository;
import com.manacommunity.api.trip.split.repository.TripSettlementPaymentRepository;
import com.manacommunity.api.trip.split.support.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** "Who owes whom": balances, the minimized settlement proposal, and recorded payments. */
@Service
@RequiredArgsConstructor
public class TripSettlementService {

    private final TripSplitAccess access;
    private final TripExpenseRepository expenseRepository;
    private final TripExpenseSplitRepository splitRepository;
    private final TripSettlementPaymentRepository paymentRepository;
    private final TripMyMoneySync myMoney;

    /** Net position of everyone on the trip. Only ACTIVE (split) expenses and CONFIRMED payments count. */
    public Map<Long, Balance> computeBalances(Trip trip, Set<Long> members) {
        List<TripExpense> active = expenseRepository.findByTripIdAndStatusIn(trip.getId(), List.of(TripExpense.ACTIVE));

        Map<Long, Map<Long, Long>> sharesByExpense = new HashMap<>();
        if (!active.isEmpty()) {
            List<Long> ids = active.stream().map(TripExpense::getId).toList();
            for (TripExpenseSplit split : splitRepository.findByExpenseIdIn(ids)) {
                sharesByExpense.computeIfAbsent(split.getExpenseId(), k -> new LinkedHashMap<>())
                        .put(split.getUserId(), split.getSharePaise());
            }
        }

        List<ExpenseLine> expenseLines = new ArrayList<>();
        for (TripExpense e : active) {
            expenseLines.add(new ExpenseLine(e.getPaidByUserId(), e.getTotalPaise(),
                    sharesByExpense.getOrDefault(e.getId(), Map.of())));
        }

        List<PaymentLine> paymentLines = new ArrayList<>();
        for (TripSettlementPayment p
                : paymentRepository.findByTripIdAndStatus(trip.getId(), TripSettlementPayment.CONFIRMED)) {
            paymentLines.add(new PaymentLine(p.getFromUserId(), p.getToUserId(), p.getAmountPaise()));
        }
        return BalanceCalculator.compute(members, expenseLines, paymentLines);
    }

    @Transactional(readOnly = true)
    public SummaryView summary(String tripId, Long actorId) {
        Trip trip = access.requireTrip(tripId);
        Set<Long> members = access.participantIds(trip);
        access.requireMember(members, actorId);

        Map<Long, Balance> balances = computeBalances(trip, members);
        Map<Long, String> names = access.names(balances.keySet());

        long spent = 0;
        long unsplit = 0;
        for (TripExpense e : expenseRepository.findByTripIdAndStatusIn(tripId,
                List.of(TripExpense.ACTIVE, TripExpense.UNSPLIT))) {
            spent += e.getTotalPaise();
            if (TripExpense.UNSPLIT.equals(e.getStatus())) {
                unsplit += e.getTotalPaise();
            }
        }

        List<BalanceView> views = new ArrayList<>();
        for (Balance b : balances.values()) {
            long net = b.netPaise();
            views.add(new BalanceView(b.userId(), names.get(b.userId()), Money.toRupees(b.paidPaise()),
                    Money.toRupees(b.sharePaise()), Money.toRupees(b.sentPaise()),
                    Money.toRupees(b.receivedPaise()), Money.toRupees(net),
                    net > 0 ? "RECEIVES" : net < 0 ? "OWES" : "SETTLED"));
        }
        return new SummaryView(tripId, Money.toRupees(spent), Money.toRupees(unsplit), views);
    }

    /** The minimized list of payments that would settle the trip right now. Computed, not stored. */
    @Transactional(readOnly = true)
    public List<TransferView> settlements(String tripId, Long actorId) {
        Trip trip = access.requireTrip(tripId);
        Set<Long> members = access.participantIds(trip);
        access.requireMember(members, actorId);

        Map<Long, Long> net = new LinkedHashMap<>();
        computeBalances(trip, members).forEach((id, b) -> net.put(id, b.netPaise()));
        List<Transfer> transfers = SettlementOptimizer.optimize(net);

        Set<Long> ids = new HashSet<>();
        transfers.forEach(t -> {
            ids.add(t.fromUserId());
            ids.add(t.toUserId());
        });
        Map<Long, String> names = access.names(ids);

        List<TransferView> views = new ArrayList<>();
        for (Transfer t : transfers) {
            views.add(new TransferView(t.fromUserId(), names.get(t.fromUserId()), t.toUserId(),
                    names.get(t.toUserId()), Money.toRupees(t.amountPaise())));
        }
        return views;
    }

    @Transactional(readOnly = true)
    public List<PaymentView> payments(String tripId, Long actorId) {
        Trip trip = access.requireTrip(tripId);
        access.requireMember(access.participantIds(trip), actorId);
        List<TripSettlementPayment> payments = paymentRepository.findByTripIdOrderByCreatedAtDesc(tripId);
        return toViews(payments);
    }

    /** The caller says "I paid this person". It affects balances only once the receiver confirms. */
    @Transactional
    public PaymentView recordPayment(String tripId, PaymentRequest req, Long actorId) {
        if (req == null) {
            throw new InvalidInputException("Payment details are required");
        }
        Trip trip = access.requireTrip(tripId);
        Set<Long> members = access.participantIds(trip);
        access.requireMember(members, actorId);

        Long toUserId = req.toUserId();
        if (toUserId == null || !members.contains(toUserId)) {
            throw new InvalidInputException("The person you paid is not a participant of this trip");
        }
        if (toUserId.equals(actorId)) {
            throw new InvalidInputException("You cannot record a payment to yourself");
        }
        long amount = Money.toPaise(req.amount());
        if (amount <= 0) {
            throw new InvalidInputException("Payment amount must be greater than zero");
        }
        if (req.reference() != null && req.reference().length() > 100) {
            throw new InvalidInputException("Reference must be at most 100 characters");
        }
        if (req.method() != null && req.method().length() > 30) {
            throw new InvalidInputException("Payment method must be at most 30 characters");
        }

        Map<Long, Balance> balances = computeBalances(trip, members);
        long owed = -balances.get(actorId).netPaise();
        long receivable = balances.get(toUserId).netPaise();
        if (owed <= 0) {
            throw new InvalidInputException("You do not owe anything on this trip");
        }
        if (amount > owed) {
            throw new InvalidInputException("Amount is more than you owe (" + Money.toRupees(owed) + ")");
        }
        if (receivable <= 0) {
            throw new InvalidInputException("That person is not owed any money on this trip");
        }
        if (amount > receivable) {
            throw new InvalidInputException("Amount is more than that person is owed (" + Money.toRupees(receivable) + ")");
        }

        TripSettlementPayment payment = paymentRepository.save(TripSettlementPayment.builder()
                .tripId(tripId)
                .fromUserId(actorId)
                .toUserId(toUserId)
                .amountPaise(amount)
                .method(req.method())
                .reference(req.reference())
                .build());
        return toViews(List.of(payment)).get(0);
    }

    @Transactional
    public PaymentView confirm(String tripId, Long paymentId, Long actorId) {
        Trip trip = access.requireTrip(tripId);
        access.requireMember(access.participantIds(trip), actorId);
        TripSettlementPayment payment = decidablePayment(trip, tripId, paymentId, actorId);

        payment.setStatus(TripSettlementPayment.CONFIRMED);
        payment.setConfirmedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        myMoney.syncPayment(trip, payment);
        return toViews(List.of(payment)).get(0);
    }

    @Transactional
    public PaymentView reject(String tripId, Long paymentId, Long actorId) {
        Trip trip = access.requireTrip(tripId);
        access.requireMember(access.participantIds(trip), actorId);
        TripSettlementPayment payment = decidablePayment(trip, tripId, paymentId, actorId);

        payment.setStatus(TripSettlementPayment.REJECTED);
        payment = paymentRepository.save(payment);
        return toViews(List.of(payment)).get(0);
    }

    /** A pending payment on this trip that the caller (the receiver, or the organizer) may decide on. */
    private TripSettlementPayment decidablePayment(Trip trip, String tripId, Long paymentId, Long actorId) {
        TripSettlementPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip payment", paymentId));
        if (!payment.getTripId().equals(tripId)) {
            throw new ResourceNotFoundException("Trip payment", paymentId);
        }
        if (!actorId.equals(payment.getToUserId()) && !access.isOrganizer(trip, actorId)) {
            throw new UnauthorizedActionException("only the person who received the money or the trip organizer can decide on this payment");
        }
        if (!TripSettlementPayment.PENDING.equals(payment.getStatus())) {
            throw new InvalidInputException("This payment has already been " + payment.getStatus().toLowerCase());
        }
        return payment;
    }

    private List<PaymentView> toViews(List<TripSettlementPayment> payments) {
        Set<Long> ids = new HashSet<>();
        payments.forEach(p -> {
            ids.add(p.getFromUserId());
            ids.add(p.getToUserId());
        });
        Map<Long, String> names = access.names(ids);

        List<PaymentView> views = new ArrayList<>();
        for (TripSettlementPayment p : payments) {
            BigDecimal amount = Money.toRupees(p.getAmountPaise());
            views.add(new PaymentView(p.getId(), p.getFromUserId(), names.get(p.getFromUserId()), p.getToUserId(),
                    names.get(p.getToUserId()), amount, p.getStatus(), p.getMethod(), p.getReference(),
                    p.getCreatedAt(), p.getConfirmedAt()));
        }
        return views;
    }
}
