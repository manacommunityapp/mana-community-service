package com.manacommunity.api.socialcircle.service;

import com.manacommunity.api.events.entity.EventExpense;
import com.manacommunity.api.events.repository.EventExpenseRepository;
import com.manacommunity.api.socialcircle.dto.KittyPartyExpenseDtos.*;
import com.manacommunity.api.socialcircle.entity.KittyPartyExpenseShare;
import com.manacommunity.api.socialcircle.repository.KittyPartyExpenseShareRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KittyPartyExpenseSplitService {

    private final KittyPartyExpenseShareRepository expenseShareRepository;
    private final EventExpenseRepository eventExpenseRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public List<ExpenseShareResponse> splitExpense(Long expenseId, SplitExpenseRequest request) {
        EventExpense expense = eventExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense record not found: " + expenseId));

        BigDecimal total = BigDecimal.valueOf(expense.getAmount());
        int count = request.getParticipantUserIds().size();
        if (count == 0) {
            throw new IllegalArgumentException("At least one participant required for splitting expense");
        }

        BigDecimal perPersonShare = (request.getCustomShareAmount() != null)
                ? request.getCustomShareAmount()
                : total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        List<ExpenseShareResponse> responses = new ArrayList<>();
        for (Long userId : request.getParticipantUserIds()) {
            AppUser user = userRepository.findById(userId).orElse(null);
            if (user == null) continue;

            KittyPartyExpenseShare share = expenseShareRepository.findByExpenseIdAndUserId(expenseId, userId)
                    .orElseGet(() -> KittyPartyExpenseShare.builder()
                            .expense(expense)
                            .user(user)
                            .build());

            share.setShareAmount(perPersonShare);
            share.setSettlementStatus("PENDING");
            KittyPartyExpenseShare saved = expenseShareRepository.save(share);

            responses.add(mapToResponse(saved));
        }

        log.info("Split expense {} among {} participants", expenseId, count);
        return responses;
    }

    @Transactional
    public ExpenseShareResponse settleShare(Long shareId, AppUser user, SettleShareRequest request) {
        KittyPartyExpenseShare share = expenseShareRepository.findById(shareId)
                .orElseThrow(() -> new IllegalArgumentException("Expense share not found"));

        if (!share.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Cannot settle an expense share belonging to another user");
        }

        share.setSettlementStatus("SETTLED");
        share.setSettlementRef(request.getSettlementRef());
        share.setSettledAt(LocalDateTime.now());
        KittyPartyExpenseShare saved = expenseShareRepository.save(share);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ExpenseShareResponse> getSharesForExpense(Long expenseId) {
        return expenseShareRepository.findByExpenseId(expenseId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseShareResponse> getMyPendingShares(Long userId) {
        return expenseShareRepository.findByUserIdAndSettlementStatus(userId, "PENDING").stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ExpenseShareResponse mapToResponse(KittyPartyExpenseShare s) {
        return ExpenseShareResponse.builder()
                .id(s.getId())
                .expenseId(s.getExpense().getId())
                .expenseDescription(s.getExpense().getDescription())
                .totalExpenseAmount(BigDecimal.valueOf(s.getExpense().getAmount()))
                .userId(s.getUser().getId())
                .userName(s.getUser().getFullName())
                .shareAmount(s.getShareAmount())
                .settlementStatus(s.getSettlementStatus())
                .settledAt(s.getSettledAt())
                .settlementRef(s.getSettlementRef())
                .build();
    }
}
