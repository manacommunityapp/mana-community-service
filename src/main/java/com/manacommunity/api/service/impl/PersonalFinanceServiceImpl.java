package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.PersonalBudgetRequest;
import com.manacommunity.api.dto.PersonalGoalRequest;
import com.manacommunity.api.dto.PersonalTransactionRequest;
import com.manacommunity.api.model.PersonalBudget;
import com.manacommunity.api.model.PersonalGoal;
import com.manacommunity.api.model.PersonalTransaction;
import com.manacommunity.api.repository.PersonalBudgetRepository;
import com.manacommunity.api.repository.PersonalGoalRepository;
import com.manacommunity.api.repository.PersonalTransactionRepository;
import com.manacommunity.api.service.PersonalFinanceService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PersonalFinanceServiceImpl implements PersonalFinanceService {

    private final PersonalTransactionRepository transactionRepository;
    private final PersonalBudgetRepository budgetRepository;
    private final PersonalGoalRepository goalRepository;

    // ── Transactions ─────────────────────────────────────────────────────

    @Override
    public List<PersonalTransaction> getTransactions(AppUser user, String type, String category,
                                                      LocalDate from, LocalDate to) {
        Long userId = user.getId();
        Long communityId = user.getCommunity().getId();

        if (from != null && to != null) {
            return transactionRepository.findByUserIdAndCommunityIdAndDateBetweenOrderByDateDesc(
                    userId, communityId, from, to);
        }
        if (type != null && !type.isBlank()) {
            return transactionRepository.findByUserIdAndCommunityIdAndTypeOrderByDateDesc(
                    userId, communityId, PersonalTransaction.TransactionType.valueOf(type.toUpperCase()));
        }
        if (category != null && !category.isBlank()) {
            return transactionRepository.findByUserIdAndCommunityIdAndCategoryOrderByDateDesc(
                    userId, communityId, category);
        }
        return transactionRepository.findByUserIdAndCommunityIdOrderByDateDesc(userId, communityId);
    }

    @Override
    @Transactional
    public PersonalTransaction createTransaction(AppUser user, PersonalTransactionRequest request) {
        PersonalTransaction txn = PersonalTransaction.builder()
                .user(user)
                .communityId(user.getCommunity().getId())
                .type(PersonalTransaction.TransactionType.valueOf(request.type().toUpperCase()))
                .category(request.category())
                .amount(request.amount())
                .description(request.description())
                .date(request.date())
                .paymentMethod(request.paymentMethod())
                .recurring(request.recurring() != null ? request.recurring() : false)
                .recurringInterval(request.recurringInterval())
                .attachmentUrl(request.attachmentUrl())
                .build();
        return transactionRepository.save(txn);
    }

    @Override
    @Transactional
    public PersonalTransaction updateTransaction(AppUser user, Long id, PersonalTransactionRequest request) {
        PersonalTransaction txn = transactionRepository.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));

        txn.setType(PersonalTransaction.TransactionType.valueOf(request.type().toUpperCase()));
        txn.setCategory(request.category());
        txn.setAmount(request.amount());
        txn.setDescription(request.description());
        txn.setDate(request.date());
        txn.setPaymentMethod(request.paymentMethod());
        txn.setRecurring(request.recurring() != null ? request.recurring() : false);
        txn.setRecurringInterval(request.recurringInterval());
        txn.setAttachmentUrl(request.attachmentUrl());
        return transactionRepository.save(txn);
    }

    @Override
    @Transactional
    public void deleteTransaction(AppUser user, Long id) {
        PersonalTransaction txn = transactionRepository.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
        transactionRepository.delete(txn);
    }

    // ── Budgets ──────────────────────────────────────────────────────────

    @Override
    public List<PersonalBudget> getBudgets(AppUser user, Integer month, Integer year) {
        if (month != null && year != null) {
            return budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year);
        }
        return budgetRepository.findByUserId(user.getId());
    }

    @Override
    @Transactional
    public PersonalBudget createBudget(AppUser user, PersonalBudgetRequest request) {
        PersonalBudget budget = PersonalBudget.builder()
                .user(user)
                .category(request.category())
                .monthlyLimit(request.monthlyLimit())
                .month(request.month())
                .year(request.year())
                .build();
        return budgetRepository.save(budget);
    }

    @Override
    @Transactional
    public PersonalBudget updateBudget(AppUser user, Long id, PersonalBudgetRequest request) {
        PersonalBudget budget = budgetRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Budget not found: " + id));
        budget.setCategory(request.category());
        budget.setMonthlyLimit(request.monthlyLimit());
        budget.setMonth(request.month());
        budget.setYear(request.year());
        return budgetRepository.save(budget);
    }

    // ── Goals ────────────────────────────────────────────────────────────

    @Override
    public List<PersonalGoal> getGoals(AppUser user) {
        return goalRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Override
    @Transactional
    public PersonalGoal createGoal(AppUser user, PersonalGoalRequest request) {
        PersonalGoal goal = PersonalGoal.builder()
                .user(user)
                .name(request.name())
                .targetAmount(request.targetAmount())
                .deadline(request.deadline())
                .build();
        return goalRepository.save(goal);
    }

    @Override
    @Transactional
    public PersonalGoal updateGoal(AppUser user, Long id, PersonalGoalRequest request) {
        PersonalGoal goal = goalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Goal not found: " + id));
        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setDeadline(request.deadline());
        if (request.status() != null) {
            goal.setStatus(PersonalGoal.GoalStatus.valueOf(request.status().toUpperCase()));
        }
        return goalRepository.save(goal);
    }

    @Override
    @Transactional
    public PersonalGoal contributeToGoal(AppUser user, Long id, BigDecimal amount) {
        PersonalGoal goal = goalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Goal not found: " + id));
        goal.setSavedAmount(goal.getSavedAmount().add(amount));
        if (goal.getSavedAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(PersonalGoal.GoalStatus.COMPLETED);
        }
        return goalRepository.save(goal);
    }

    // ── Summary ──────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> getMonthlySummary(AppUser user, int month, int year) {
        Long userId = user.getId();
        Long communityId = user.getCommunity().getId();

        BigDecimal income = transactionRepository.sumByUserAndTypeAndMonthYear(
                userId, communityId, PersonalTransaction.TransactionType.INCOME, month, year);
        BigDecimal expense = transactionRepository.sumByUserAndTypeAndMonthYear(
                userId, communityId, PersonalTransaction.TransactionType.EXPENSE, month, year);
        BigDecimal savings = income.subtract(expense);

        Map<String, Object> summary = new HashMap<>();
        summary.put("month", month);
        summary.put("year", year);
        summary.put("totalIncome", income);
        summary.put("totalExpense", expense);
        summary.put("savings", savings);
        return summary;
    }
}
