package com.manacommunity.api.finance.personal.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.finance.personal.dto.*;
import com.manacommunity.api.finance.personal.entity.*;
import com.manacommunity.api.finance.personal.repository.*;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalFinanceService {

    private final FinanceAccountRepository accountRepo;
    private final FinanceTransactionRepository txnRepo;
    private final FinanceCategoryRepository categoryRepo;
    private final FinanceBudgetRepository budgetRepo;
    private final FinanceBillRepository billRepo;
    private final FinanceRecurringTxnRepository recurringRepo;

    // ── Accounts ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(Long userId) {
        return accountRepo.findByUserIdAndActiveTrueOrderByAccountNameAsc(userId)
                .stream().map(this::toAccountResponse).toList();
    }

    @Transactional
    public AccountResponse createAccount(AppUser user, AccountRequest req) {
        FinanceAccount account = FinanceAccount.builder()
                .user(user)
                .accountName(req.accountName())
                .accountType(parseAccountType(req.accountType()))
                .balance(req.balance() != null ? req.balance() : BigDecimal.ZERO)
                .currency(req.currency() != null ? req.currency() : "INR")
                .institution(req.institution())
                .accountNumberMasked(req.accountNumberMasked())
                .color(req.color())
                .icon(req.icon())
                .includeInTotal(req.includeInTotal() != null ? req.includeInTotal() : true)
                .build();
        return toAccountResponse(accountRepo.save(account));
    }

    @Transactional
    public AccountResponse updateAccount(Long userId, Long accountId, AccountRequest req) {
        FinanceAccount account = findAccountOwned(userId, accountId);
        account.setAccountName(req.accountName());
        account.setAccountType(parseAccountType(req.accountType()));
        if (req.currency() != null) account.setCurrency(req.currency());
        account.setInstitution(req.institution());
        account.setAccountNumberMasked(req.accountNumberMasked());
        account.setColor(req.color());
        account.setIcon(req.icon());
        if (req.includeInTotal() != null) account.setIncludeInTotal(req.includeInTotal());
        return toAccountResponse(accountRepo.save(account));
    }

    @Transactional
    public void deleteAccount(Long userId, Long accountId) {
        FinanceAccount account = findAccountOwned(userId, accountId);
        account.setActive(false);
        accountRepo.save(account);
    }

    // ── Transactions ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(Long userId, Long accountId, Pageable pageable) {
        if (accountId != null) {
            return txnRepo.findByUserIdAndAccountIdOrderByTxnDateDescCreatedAtDesc(userId, accountId, pageable)
                    .map(this::toTxnResponse);
        }
        return txnRepo.findByUserIdOrderByTxnDateDescCreatedAtDesc(userId, pageable)
                .map(this::toTxnResponse);
    }

    @Transactional
    public TransactionResponse createTransaction(AppUser user, TransactionRequest req) {
        FinanceAccount account = findAccountOwned(user.getId(), req.accountId());
        FinanceTransaction.TxnType txnType = parseTxnType(req.txnType());

        FinanceCategory category = null;
        if (req.categoryId() != null) {
            category = categoryRepo.findById(req.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));
        }

        FinanceAccount transferTo = null;
        if (txnType == FinanceTransaction.TxnType.TRANSFER) {
            if (req.transferToAccountId() == null) {
                throw new InvalidInputException("Transfer requires a destination account.");
            }
            transferTo = findAccountOwned(user.getId(), req.transferToAccountId());
            if (transferTo.getId().equals(account.getId())) {
                throw new InvalidInputException("Cannot transfer to the same account.");
            }
        }

        FinanceTransaction txn = FinanceTransaction.builder()
                .user(user)
                .account(account)
                .txnType(txnType)
                .amount(req.amount())
                .category(category)
                .txnDate(req.txnDate())
                .description(req.description())
                .payee(req.payee())
                .transferToAccount(transferTo)
                .notes(req.notes())
                .build();

        FinanceTransaction saved = txnRepo.save(txn);
        updateAccountBalance(account, txnType, req.amount(), true);
        if (transferTo != null) {
            updateAccountBalance(transferTo, FinanceTransaction.TxnType.INCOME, req.amount(), true);
        }

        return toTxnResponse(saved);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long userId, Long txnId, TransactionRequest req) {
        FinanceTransaction txn = txnRepo.findById(txnId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", txnId));
        requireOwner(txn.getUser().getId(), userId);

        updateAccountBalance(txn.getAccount(), txn.getTxnType(), txn.getAmount(), false);
        if (txn.getTransferToAccount() != null) {
            updateAccountBalance(txn.getTransferToAccount(), FinanceTransaction.TxnType.INCOME, txn.getAmount(), false);
        }

        FinanceAccount account = findAccountOwned(userId, req.accountId());
        FinanceTransaction.TxnType txnType = parseTxnType(req.txnType());

        FinanceCategory category = null;
        if (req.categoryId() != null) {
            category = categoryRepo.findById(req.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));
        }

        FinanceAccount transferTo = null;
        if (txnType == FinanceTransaction.TxnType.TRANSFER && req.transferToAccountId() != null) {
            transferTo = findAccountOwned(userId, req.transferToAccountId());
        }

        txn.setAccount(account);
        txn.setTxnType(txnType);
        txn.setAmount(req.amount());
        txn.setCategory(category);
        txn.setTxnDate(req.txnDate());
        txn.setDescription(req.description());
        txn.setPayee(req.payee());
        txn.setTransferToAccount(transferTo);
        txn.setNotes(req.notes());

        FinanceTransaction saved = txnRepo.save(txn);
        updateAccountBalance(account, txnType, req.amount(), true);
        if (transferTo != null) {
            updateAccountBalance(transferTo, FinanceTransaction.TxnType.INCOME, req.amount(), true);
        }

        return toTxnResponse(saved);
    }

    @Transactional
    public void deleteTransaction(Long userId, Long txnId) {
        FinanceTransaction txn = txnRepo.findById(txnId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", txnId));
        requireOwner(txn.getUser().getId(), userId);

        updateAccountBalance(txn.getAccount(), txn.getTxnType(), txn.getAmount(), false);
        if (txn.getTransferToAccount() != null) {
            updateAccountBalance(txn.getTransferToAccount(), FinanceTransaction.TxnType.INCOME, txn.getAmount(), false);
        }

        txnRepo.delete(txn);
    }

    // ── Categories ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(Long userId) {
        return categoryRepo.findByUserIdOrSystem(userId)
                .stream().map(this::toCategoryResponse).toList();
    }

    @Transactional
    public CategoryResponse createCategory(AppUser user, CategoryRequest req) {
        FinanceCategory category = FinanceCategory.builder()
                .user(user)
                .name(req.name())
                .categoryType(parseCategoryType(req.categoryType()))
                .icon(req.icon())
                .color(req.color())
                .sortOrder(req.sortOrder() != null ? req.sortOrder() : 0)
                .build();
        return toCategoryResponse(categoryRepo.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long userId, Long categoryId, CategoryRequest req) {
        FinanceCategory category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        if (category.isSystem()) {
            throw new InvalidInputException("System categories cannot be modified.");
        }
        requireOwner(category.getUser().getId(), userId);

        category.setName(req.name());
        category.setCategoryType(parseCategoryType(req.categoryType()));
        category.setIcon(req.icon());
        category.setColor(req.color());
        if (req.sortOrder() != null) category.setSortOrder(req.sortOrder());
        return toCategoryResponse(categoryRepo.save(category));
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        FinanceCategory category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        if (category.isSystem()) {
            throw new InvalidInputException("System categories cannot be deleted.");
        }
        requireOwner(category.getUser().getId(), userId);
        categoryRepo.delete(category);
    }

    // ── Budgets ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(Long userId, int year, Integer month) {
        List<FinanceBudget> budgets;
        if (month != null) {
            budgets = budgetRepo.findByUserIdAndBudgetYearAndBudgetMonthOrderByCategoryNameAsc(userId, year, month);
        } else {
            budgets = budgetRepo.findByUserIdAndBudgetYearOrderByCategoryNameAsc(userId, year);
        }
        return budgets.stream().map(b -> toBudgetResponse(b, userId)).toList();
    }

    @Transactional
    public BudgetResponse createBudget(AppUser user, BudgetRequest req) {
        FinanceCategory category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));

        FinanceBudget.BudgetPeriod period = req.period() != null
                ? FinanceBudget.BudgetPeriod.valueOf(req.period().toUpperCase())
                : FinanceBudget.BudgetPeriod.MONTHLY;

        budgetRepo.findByUserIdAndCategoryIdAndBudgetYearAndBudgetMonth(
                user.getId(), req.categoryId(), req.budgetYear(), req.budgetMonth()
        ).ifPresent(existing -> {
            throw new InvalidInputException("Budget already exists for this category/period.");
        });

        FinanceBudget budget = FinanceBudget.builder()
                .user(user)
                .category(category)
                .budgetAmount(req.budgetAmount())
                .period(period)
                .budgetYear(req.budgetYear())
                .budgetMonth(req.budgetMonth())
                .alertThresholdPct(req.alertThresholdPct() != null ? req.alertThresholdPct() : 80)
                .build();

        return toBudgetResponse(budgetRepo.save(budget), user.getId());
    }

    @Transactional
    public BudgetResponse updateBudget(Long userId, Long budgetId, BudgetRequest req) {
        FinanceBudget budget = budgetRepo.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", budgetId));
        requireOwner(budget.getUser().getId(), userId);

        budget.setBudgetAmount(req.budgetAmount());
        if (req.alertThresholdPct() != null) budget.setAlertThresholdPct(req.alertThresholdPct());
        return toBudgetResponse(budgetRepo.save(budget), userId);
    }

    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        FinanceBudget budget = budgetRepo.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", budgetId));
        requireOwner(budget.getUser().getId(), userId);
        budgetRepo.delete(budget);
    }

    // ── Bills ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BillResponse> getBills(Long userId, String status) {
        if (status != null && !status.isBlank()) {
            return billRepo.findByUserIdAndStatusOrderByDueDateAsc(userId, FinanceBill.BillStatus.valueOf(status.toUpperCase()))
                    .stream().map(this::toBillResponse).toList();
        }
        return billRepo.findByUserIdOrderByDueDateAsc(userId)
                .stream().map(this::toBillResponse).toList();
    }

    @Transactional
    public BillResponse createBill(AppUser user, BillRequest req) {
        FinanceCategory category = null;
        if (req.categoryId() != null) {
            category = categoryRepo.findById(req.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));
        }

        FinanceBill bill = FinanceBill.builder()
                .user(user)
                .name(req.name())
                .amount(req.amount())
                .dueDate(req.dueDate())
                .category(category)
                .recurring(req.recurring() != null && req.recurring())
                .recurrencePeriod(req.recurrencePeriod() != null
                        ? FinanceBill.RecurrencePeriod.valueOf(req.recurrencePeriod().toUpperCase()) : null)
                .autoPay(req.autoPay() != null && req.autoPay())
                .notes(req.notes())
                .build();

        return toBillResponse(billRepo.save(bill));
    }

    @Transactional
    public BillResponse updateBill(Long userId, Long billId, BillRequest req) {
        FinanceBill bill = billRepo.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill", billId));
        requireOwner(bill.getUser().getId(), userId);

        FinanceCategory category = null;
        if (req.categoryId() != null) {
            category = categoryRepo.findById(req.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));
        }

        bill.setName(req.name());
        bill.setAmount(req.amount());
        bill.setDueDate(req.dueDate());
        bill.setCategory(category);
        if (req.recurring() != null) bill.setRecurring(req.recurring());
        if (req.recurrencePeriod() != null) {
            bill.setRecurrencePeriod(FinanceBill.RecurrencePeriod.valueOf(req.recurrencePeriod().toUpperCase()));
        }
        if (req.autoPay() != null) bill.setAutoPay(req.autoPay());
        bill.setNotes(req.notes());

        return toBillResponse(billRepo.save(bill));
    }

    @Transactional
    public BillResponse markBillPaid(Long userId, Long billId) {
        FinanceBill bill = billRepo.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill", billId));
        requireOwner(bill.getUser().getId(), userId);
        bill.setStatus(FinanceBill.BillStatus.PAID);
        return toBillResponse(billRepo.save(bill));
    }

    @Transactional
    public void deleteBill(Long userId, Long billId) {
        FinanceBill bill = billRepo.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill", billId));
        requireOwner(bill.getUser().getId(), userId);
        billRepo.delete(bill);
    }

    // ── Recurring Transactions ──────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RecurringTxnResponse> getRecurringTxns(Long userId) {
        return recurringRepo.findByUserIdOrderByNextDueDateAsc(userId)
                .stream().map(this::toRecurringResponse).toList();
    }

    @Transactional
    public RecurringTxnResponse createRecurringTxn(AppUser user, RecurringTxnRequest req) {
        FinanceAccount account = findAccountOwned(user.getId(), req.accountId());

        FinanceCategory category = null;
        if (req.categoryId() != null) {
            category = categoryRepo.findById(req.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));
        }

        FinanceRecurringTxn recurring = FinanceRecurringTxn.builder()
                .user(user)
                .account(account)
                .txnType(parseTxnType(req.txnType()))
                .amount(req.amount())
                .category(category)
                .description(req.description())
                .payee(req.payee())
                .frequency(FinanceBill.RecurrencePeriod.valueOf(req.frequency().toUpperCase()))
                .startDate(req.startDate())
                .endDate(req.endDate())
                .nextDueDate(req.startDate())
                .build();

        return toRecurringResponse(recurringRepo.save(recurring));
    }

    @Transactional
    public void deleteRecurringTxn(Long userId, Long id) {
        FinanceRecurringTxn recurring = recurringRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RecurringTransaction", id));
        requireOwner(recurring.getUser().getId(), userId);
        recurring.setActive(false);
        recurringRepo.save(recurring);
    }

    // ── Summary / Dashboard ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public FinanceSummaryResponse getSummary(Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        BigDecimal totalBalance = accountRepo.sumBalanceByUserId(userId);
        BigDecimal monthIncome = txnRepo.sumByUserAndTypeAndDateRange(userId, FinanceTransaction.TxnType.INCOME, monthStart, monthEnd);
        BigDecimal monthExpenses = txnRepo.sumByUserAndTypeAndDateRange(userId, FinanceTransaction.TxnType.EXPENSE, monthStart, monthEnd);
        int totalAccounts = accountRepo.countByUserIdAndActiveTrue(userId);
        int pendingBills = billRepo.countByUserIdAndStatus(userId, FinanceBill.BillStatus.PENDING);
        BigDecimal pendingBillsAmount = billRepo.sumPendingAmount(userId);

        List<Object[]> categoryBreakdowns = txnRepo.sumExpensesByCategory(userId, monthStart, monthEnd);
        BigDecimal totalExpForPct = monthExpenses.compareTo(BigDecimal.ZERO) > 0 ? monthExpenses : BigDecimal.ONE;
        List<FinanceSummaryResponse.CategoryBreakdown> topCategories = new ArrayList<>();
        for (int i = 0; i < Math.min(5, categoryBreakdowns.size()); i++) {
            Object[] row = categoryBreakdowns.get(i);
            BigDecimal amt = (BigDecimal) row[2];
            topCategories.add(FinanceSummaryResponse.CategoryBreakdown.builder()
                    .categoryName((String) row[1])
                    .amount(amt)
                    .percentage(amt.divide(totalExpForPct, 4, RoundingMode.HALF_UP).doubleValue() * 100)
                    .build());
        }

        return FinanceSummaryResponse.builder()
                .totalBalance(totalBalance)
                .monthIncome(monthIncome)
                .monthExpenses(monthExpenses)
                .monthSavings(monthIncome.subtract(monthExpenses))
                .totalAccounts(totalAccounts)
                .pendingBills(pendingBills)
                .pendingBillsAmount(pendingBillsAmount)
                .topExpenseCategories(topCategories)
                .build();
    }

    // ── Private helpers ─────────────────────────────────────────────

    private FinanceAccount findAccountOwned(Long userId, Long accountId) {
        FinanceAccount account = accountRepo.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
        requireOwner(account.getUser().getId(), userId);
        return account;
    }

    private void requireOwner(Long ownerId, Long callerId) {
        if (!ownerId.equals(callerId)) {
            throw new InvalidInputException("You do not have access to this resource.");
        }
    }

    private void updateAccountBalance(FinanceAccount account, FinanceTransaction.TxnType txnType, BigDecimal amount, boolean add) {
        BigDecimal delta = add ? amount : amount.negate();
        switch (txnType) {
            case INCOME -> account.setBalance(account.getBalance().add(delta));
            case EXPENSE -> account.setBalance(account.getBalance().subtract(delta));
            case TRANSFER -> account.setBalance(account.getBalance().subtract(delta));
        }
        accountRepo.save(account);
    }

    private FinanceAccount.AccountType parseAccountType(String raw) {
        if (raw == null || raw.isBlank()) return FinanceAccount.AccountType.BANK;
        try { return FinanceAccount.AccountType.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) { return FinanceAccount.AccountType.OTHER; }
    }

    private FinanceTransaction.TxnType parseTxnType(String raw) {
        if (raw == null || raw.isBlank()) throw new InvalidInputException("Transaction type is required.");
        try { return FinanceTransaction.TxnType.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) { throw new InvalidInputException("Invalid transaction type: " + raw); }
    }

    private FinanceCategory.CategoryType parseCategoryType(String raw) {
        if (raw == null || raw.isBlank()) throw new InvalidInputException("Category type is required.");
        try { return FinanceCategory.CategoryType.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) { throw new InvalidInputException("Invalid category type: " + raw); }
    }

    private AccountResponse toAccountResponse(FinanceAccount a) {
        return AccountResponse.builder()
                .id(a.getId())
                .accountName(a.getAccountName())
                .accountType(a.getAccountType().name())
                .balance(a.getBalance())
                .currency(a.getCurrency())
                .institution(a.getInstitution())
                .accountNumberMasked(a.getAccountNumberMasked())
                .color(a.getColor())
                .icon(a.getIcon())
                .active(a.isActive())
                .includeInTotal(a.isIncludeInTotal())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private TransactionResponse toTxnResponse(FinanceTransaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .accountId(t.getAccount().getId())
                .accountName(t.getAccount().getAccountName())
                .txnType(t.getTxnType().name())
                .amount(t.getAmount())
                .categoryId(t.getCategory() != null ? t.getCategory().getId() : null)
                .categoryName(t.getCategory() != null ? t.getCategory().getName() : null)
                .txnDate(t.getTxnDate())
                .description(t.getDescription())
                .payee(t.getPayee())
                .transferToAccountId(t.getTransferToAccount() != null ? t.getTransferToAccount().getId() : null)
                .transferToAccountName(t.getTransferToAccount() != null ? t.getTransferToAccount().getAccountName() : null)
                .sourceModule(t.getSourceModule())
                .sourceRefId(t.getSourceRefId())
                .notes(t.getNotes())
                .recurringInstance(t.isRecurringInstance())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private CategoryResponse toCategoryResponse(FinanceCategory c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .categoryType(c.getCategoryType().name())
                .icon(c.getIcon())
                .color(c.getColor())
                .system(c.isSystem())
                .sortOrder(c.getSortOrder())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private BudgetResponse toBudgetResponse(FinanceBudget b, Long userId) {
        LocalDate from;
        LocalDate to;
        if (b.getBudgetMonth() != null) {
            from = LocalDate.of(b.getBudgetYear(), b.getBudgetMonth(), 1);
            to = from.withDayOfMonth(from.lengthOfMonth());
        } else {
            from = LocalDate.of(b.getBudgetYear(), 1, 1);
            to = LocalDate.of(b.getBudgetYear(), 12, 31);
        }
        BigDecimal spent = txnRepo.sumExpensesByCategoryAndDateRange(userId, b.getCategory().getId(), from, to);

        return BudgetResponse.builder()
                .id(b.getId())
                .categoryId(b.getCategory().getId())
                .categoryName(b.getCategory().getName())
                .budgetAmount(b.getBudgetAmount())
                .spentAmount(spent)
                .period(b.getPeriod().name())
                .budgetYear(b.getBudgetYear())
                .budgetMonth(b.getBudgetMonth())
                .alertThresholdPct(b.getAlertThresholdPct())
                .createdAt(b.getCreatedAt())
                .build();
    }

    private BillResponse toBillResponse(FinanceBill b) {
        return BillResponse.builder()
                .id(b.getId())
                .name(b.getName())
                .amount(b.getAmount())
                .dueDate(b.getDueDate())
                .categoryId(b.getCategory() != null ? b.getCategory().getId() : null)
                .categoryName(b.getCategory() != null ? b.getCategory().getName() : null)
                .status(b.getStatus().name())
                .recurring(b.isRecurring())
                .recurrencePeriod(b.getRecurrencePeriod() != null ? b.getRecurrencePeriod().name() : null)
                .autoPay(b.isAutoPay())
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .build();
    }

    private RecurringTxnResponse toRecurringResponse(FinanceRecurringTxn r) {
        return RecurringTxnResponse.builder()
                .id(r.getId())
                .accountId(r.getAccount().getId())
                .accountName(r.getAccount().getAccountName())
                .txnType(r.getTxnType().name())
                .amount(r.getAmount())
                .categoryId(r.getCategory() != null ? r.getCategory().getId() : null)
                .categoryName(r.getCategory() != null ? r.getCategory().getName() : null)
                .description(r.getDescription())
                .payee(r.getPayee())
                .frequency(r.getFrequency().name())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .nextDueDate(r.getNextDueDate())
                .active(r.isActive())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
