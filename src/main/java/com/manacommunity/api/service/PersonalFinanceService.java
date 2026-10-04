package com.manacommunity.api.service;

import com.manacommunity.api.dto.PersonalBudgetRequest;
import com.manacommunity.api.dto.PersonalGoalRequest;
import com.manacommunity.api.dto.PersonalTransactionRequest;
import com.manacommunity.api.model.PersonalBudget;
import com.manacommunity.api.model.PersonalGoal;
import com.manacommunity.api.model.PersonalTransaction;
import com.manacommunity.api.user.model.AppUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface PersonalFinanceService {

    // Transactions
    List<PersonalTransaction> getTransactions(AppUser user, String type, String category, LocalDate from, LocalDate to);
    PersonalTransaction createTransaction(AppUser user, PersonalTransactionRequest request);
    PersonalTransaction updateTransaction(AppUser user, Long id, PersonalTransactionRequest request);
    void deleteTransaction(AppUser user, Long id);

    // Budgets
    List<PersonalBudget> getBudgets(AppUser user, Integer month, Integer year);
    PersonalBudget createBudget(AppUser user, PersonalBudgetRequest request);
    PersonalBudget updateBudget(AppUser user, Long id, PersonalBudgetRequest request);

    // Goals
    List<PersonalGoal> getGoals(AppUser user);
    PersonalGoal createGoal(AppUser user, PersonalGoalRequest request);
    PersonalGoal updateGoal(AppUser user, Long id, PersonalGoalRequest request);
    PersonalGoal contributeToGoal(AppUser user, Long id, BigDecimal amount);

    // Summary
    Map<String, Object> getMonthlySummary(AppUser user, int month, int year);
}
