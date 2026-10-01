package com.manacommunity.api.finance.personal.service;

import com.manacommunity.api.finance.personal.dto.*;
import com.manacommunity.api.user.model.AppUser;

import java.math.BigDecimal;
import java.util.List;

public interface PersonalFinanceService {
    List<PersonalAccountDto> getAccounts(AppUser user);
    PersonalAccountDto createAccount(CreatePersonalAccountDto dto, AppUser user);
    PersonalAccountDto updateAccount(String id, CreatePersonalAccountDto dto, AppUser user);
    List<PersonalCategoryDto> getCategories(AppUser user);
    PersonalCategoryDto createCategory(CreatePersonalCategoryDto dto, AppUser user);
    List<PersonalTransactionDto> getTransactions(AppUser user, String type, String categoryId, String accountId, String from, String to, String tag, int page, int limit);
    CreatePersonalTransactionDto parseNaturalLanguageText(String text, AppUser user);
    PersonalTransactionDto createTransaction(CreatePersonalTransactionDto dto, AppUser user);
    void deleteTransaction(String id, AppUser user);
    PersonalDashboardSummaryDto getDashboardSummary(AppUser user, String month);
    List<PersonalBudgetDto> getBudgets(AppUser user, String month);
    PersonalBudgetDto createBudget(CreatePersonalBudgetDto dto, AppUser user);
    List<PersonalRecurringDto> getRecurringTransactions(AppUser user);
    PersonalRecurringDto createRecurring(CreatePersonalRecurringDto dto, AppUser user);
    PersonalRecurringDto toggleRecurring(String id, AppUser user);
    List<PersonalBillDto> getBills(AppUser user);
    void markBillPaid(String id, AppUser user);
    PersonalReportPeriodDto getReport(AppUser user, String period);
    List<PersonalTransactionDto> getManaProjections(AppUser user);

    // P3 Enhancements
    List<PersonalInstallmentDto> getInstallments(AppUser user);
    PersonalInstallmentDto createInstallment(CreatePersonalInstallmentDto dto, AppUser user);
    PersonalInstallmentDto payInstallment(String id, BigDecimal amount, String accountId, AppUser user);
    void deleteInstallment(String id, AppUser user);

    List<PersonalGoalDto> getGoals(AppUser user);
    PersonalGoalDto createGoal(CreatePersonalGoalDto dto, AppUser user);
    PersonalGoalDto contributeToGoal(String id, GoalContributionDto dto, AppUser user);
    void deleteGoal(String id, AppUser user);

    BatchImportResultDto batchImportTransactions(BatchImportTransactionDto dto, AppUser user);
    int processDueRecurring(AppUser user);
    int processAllDueRecurring();
}
