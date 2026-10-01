package com.manacommunity.api.finance.personal.service.impl;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.finance.personal.dto.*;
import com.manacommunity.api.finance.personal.entity.*;
import com.manacommunity.api.finance.personal.repository.*;
import com.manacommunity.api.finance.personal.service.PersonalFinanceService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalFinanceServiceImpl implements PersonalFinanceService {

    private final PersonalAccountRepository accountRepository;
    private final PersonalCategoryRepository categoryRepository;
    private final PersonalTransactionRepository transactionRepository;
    private final PersonalBudgetRepository budgetRepository;
    private final PersonalBillRepository billRepository;
    private final PersonalRecurringRepository recurringRepository;
    private final PersonalInstallmentRepository installmentRepository;
    private final PersonalGoalRepository goalRepository;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DISPLAY_MONTH_FMT = DateTimeFormatter.ofPattern("MMM");

    // ─── Accounts ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PersonalAccountDto> getAccounts(AppUser user) {
        List<PersonalAccount> accounts = accountRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }
        return accounts.stream().map(this::toAccountDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PersonalAccountDto createAccount(CreatePersonalAccountDto dto, AppUser user) {
        String id = "acc-" + UUID.randomUUID().toString().substring(0, 8);
        PersonalAccount account = PersonalAccount.builder()
                .id(id)
                .user(user)
                .name(dto.getName())
                .type(dto.getType() != null ? dto.getType() : "SAVINGS")
                .balance(dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO)
                .creditLimit(dto.getCreditLimit())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "₹")
                .bankName(dto.getBankName())
                .accountNumber(dto.getAccountNumber())
                .billingDay(dto.getBillingDay())
                .paymentDueDay(dto.getPaymentDueDay())
                .color(dto.getColor() != null ? dto.getColor() : "#3B82F6")
                .icon(dto.getIcon() != null ? dto.getIcon() : "wallet-outline")
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
        return toAccountDto(accountRepository.save(account));
    }

    @Override
    @Transactional
    public PersonalAccountDto updateAccount(String id, CreatePersonalAccountDto dto, AppUser user) {
        PersonalAccount account = accountRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));

        account.setName(dto.getName());
        account.setType(dto.getType());
        if (dto.getBalance() != null) account.setBalance(dto.getBalance());
        account.setCreditLimit(dto.getCreditLimit());
        if (dto.getCurrency() != null) account.setCurrency(dto.getCurrency());
        account.setBankName(dto.getBankName());
        account.setAccountNumber(dto.getAccountNumber());
        account.setBillingDay(dto.getBillingDay());
        account.setPaymentDueDay(dto.getPaymentDueDay());
        if (dto.getColor() != null) account.setColor(dto.getColor());
        if (dto.getIcon() != null) account.setIcon(dto.getIcon());
        if (dto.getIsActive() != null) account.setIsActive(dto.getIsActive());

        return toAccountDto(accountRepository.save(account));
    }

    // ─── Categories ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PersonalCategoryDto> getCategories(AppUser user) {
        List<PersonalCategory> all = categoryRepository.findAllForUser(user.getId());
        Map<String, List<PersonalSubcategoryDto>> subMap = all.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(PersonalCategory::getParentId,
                        Collectors.mapping(c -> PersonalSubcategoryDto.builder()
                                .id(c.getId())
                                .name(c.getName())
                                .build(), Collectors.toList())));

        return all.stream()
                .filter(c -> c.getParentId() == null)
                .map(c -> PersonalCategoryDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .icon(c.getIcon())
                        .color(c.getColor())
                        .type(c.getType())
                        .subcategories(subMap.getOrDefault(c.getId(), Collections.emptyList()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PersonalCategoryDto createCategory(CreatePersonalCategoryDto dto, AppUser user) {
        String id = "cat-" + UUID.randomUUID().toString().substring(0, 8);
        PersonalCategory cat = PersonalCategory.builder()
                .id(id)
                .user(user)
                .name(dto.getName())
                .type(dto.getType() != null ? dto.getType() : "EXPENSE")
                .icon(dto.getIcon() != null ? dto.getIcon() : "pricetag-outline")
                .color(dto.getColor() != null ? dto.getColor() : "#64748B")
                .parentId(dto.getParentId())
                .build();
        categoryRepository.save(cat);

        List<PersonalSubcategoryDto> subDtos = new ArrayList<>();
        if (dto.getSubcategories() != null) {
            for (String sub : dto.getSubcategories()) {
                String subId = "sub-" + UUID.randomUUID().toString().substring(0, 8);
                PersonalCategory subCat = PersonalCategory.builder()
                        .id(subId)
                        .user(user)
                        .name(sub)
                        .type(cat.getType())
                        .icon(cat.getIcon())
                        .color(cat.getColor())
                        .parentId(cat.getId())
                        .build();
                categoryRepository.save(subCat);
                subDtos.add(PersonalSubcategoryDto.builder().id(subId).name(sub).build());
            }
        }

        return PersonalCategoryDto.builder()
                .id(cat.getId())
                .name(cat.getName())
                .icon(cat.getIcon())
                .color(cat.getColor())
                .type(cat.getType())
                .subcategories(subDtos)
                .build();
    }

    // ─── Transactions ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PersonalTransactionDto> getTransactions(AppUser user, String type, String categoryId, String accountId, String from, String to, String tag, int page, int limit) {
        LocalDate fromDate = (from != null && !from.isBlank()) ? LocalDate.parse(from) : null;
        LocalDate toDate = (to != null && !to.isBlank()) ? LocalDate.parse(to) : null;
        Pageable pageable = PageRequest.of(page, limit);

        List<PersonalTransaction> txns;
        if (fromDate != null && toDate != null) {
            txns = transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(user.getId(), fromDate, toDate);
        } else {
            txns = transactionRepository.findByUserIdOrderByTransactionDateDescCreatedAtDesc(user.getId(), pageable);
        }

        return txns.stream()
                .filter(t -> type == null || type.isBlank() || t.getType().equalsIgnoreCase(type))
                .filter(t -> categoryId == null || categoryId.isBlank() || categoryId.equals(t.getCategoryId()))
                .filter(t -> accountId == null || accountId.isBlank() || accountId.equals(t.getAccountId()) || accountId.equals(t.getToAccountId()))
                .filter(t -> tag == null || tag.isBlank() || (t.getTags() != null && t.getTags().toLowerCase().contains(tag.toLowerCase())))
                .map(this::toTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PersonalTransactionDto createTransaction(CreatePersonalTransactionDto dto, AppUser user) {
        PersonalAccount fromAccount = accountRepository.findByIdAndUserId(dto.getAccountId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + dto.getAccountId()));

        PersonalAccount toAccount = null;
        if ("TRANSFER".equalsIgnoreCase(dto.getType()) && dto.getToAccountId() != null) {
            toAccount = accountRepository.findByIdAndUserId(dto.getToAccountId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("To-Account not found: " + dto.getToAccountId()));
        }

        PersonalCategory category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
        }

        String txnId = "txn-" + UUID.randomUUID().toString().substring(0, 8);
        LocalDate date = dto.getDate() != null ? LocalDate.parse(dto.getDate()) : LocalDate.now();

        PersonalTransaction txn = PersonalTransaction.builder()
                .id(txnId)
                .user(user)
                .type(dto.getType())
                .amount(dto.getAmount())
                .currency(fromAccount.getCurrency())
                .accountId(fromAccount.getId())
                .accountName(fromAccount.getName())
                .toAccountId(toAccount != null ? toAccount.getId() : null)
                .toAccountName(toAccount != null ? toAccount.getName() : null)
                .categoryId(category != null ? category.getId() : dto.getCategoryId())
                .categoryName(category != null ? category.getName() : "General")
                .categoryIcon(category != null ? category.getIcon() : "receipt-outline")
                .categoryColor(category != null ? category.getColor() : "#64748B")
                .subcategoryName(dto.getSubcategoryName())
                .description(dto.getDescription())
                .notes(dto.getNotes())
                .receiptUrl(dto.getReceiptUrl())
                .tags(dto.getTags())
                .splitDetails(dto.getSplitDetails())
                .transactionDate(date)
                .isManaProjection(false)
                .build();

        // Atomic Balance Update
        BigDecimal amt = dto.getAmount();
        if ("INCOME".equalsIgnoreCase(dto.getType())) {
            fromAccount.setBalance(fromAccount.getBalance().add(amt));
            accountRepository.save(fromAccount);
        } else if ("EXPENSE".equalsIgnoreCase(dto.getType())) {
            fromAccount.setBalance(fromAccount.getBalance().subtract(amt));
            accountRepository.save(fromAccount);
        } else if ("TRANSFER".equalsIgnoreCase(dto.getType()) && toAccount != null) {
            fromAccount.setBalance(fromAccount.getBalance().subtract(amt));
            toAccount.setBalance(toAccount.getBalance().add(amt));
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);
        }

        return toTransactionDto(transactionRepository.save(txn));
    }

    @Override
    @Transactional
    public void deleteTransaction(String id, AppUser user) {
        PersonalTransaction txn = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));

        // Reversal of balance changes
        Optional<PersonalAccount> fromAccOpt = accountRepository.findByIdAndUserId(txn.getAccountId(), user.getId());
        if (fromAccOpt.isPresent()) {
            PersonalAccount fromAcc = fromAccOpt.get();
            BigDecimal amt = txn.getAmount();
            if ("INCOME".equalsIgnoreCase(txn.getType())) {
                fromAcc.setBalance(fromAcc.getBalance().subtract(amt));
                accountRepository.save(fromAcc);
            } else if ("EXPENSE".equalsIgnoreCase(txn.getType())) {
                fromAcc.setBalance(fromAcc.getBalance().add(amt));
                accountRepository.save(fromAcc);
            } else if ("TRANSFER".equalsIgnoreCase(txn.getType()) && txn.getToAccountId() != null) {
                fromAcc.setBalance(fromAcc.getBalance().add(amt));
                accountRepository.save(fromAcc);
                accountRepository.findByIdAndUserId(txn.getToAccountId(), user.getId()).ifPresent(toAcc -> {
                    toAcc.setBalance(toAcc.getBalance().subtract(amt));
                    accountRepository.save(toAcc);
                });
            }
        }

        transactionRepository.delete(txn);
    }

    // ─── Dashboard Summary ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PersonalDashboardSummaryDto getDashboardSummary(AppUser user, String month) {
        YearMonth ym = (month != null && !month.isBlank()) ? YearMonth.parse(month) : YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        BigDecimal income = transactionRepository.sumIncomeForPeriod(user.getId(), start, end);
        if (income == null) income = BigDecimal.ZERO;

        BigDecimal expense = transactionRepository.sumExpensesForPeriod(user.getId(), start, end);
        if (expense == null) expense = BigDecimal.ZERO;

        BigDecimal savings = income.subtract(expense);
        int savingsRate = income.compareTo(BigDecimal.ZERO) > 0
                ? savings.multiply(BigDecimal.valueOf(100)).divide(income, 0, RoundingMode.HALF_UP).intValue()
                : 0;

        List<PersonalAccount> accounts = accountRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
        BigDecimal assets = BigDecimal.ZERO;
        BigDecimal liabilities = BigDecimal.ZERO;
        for (PersonalAccount acc : accounts) {
            if ("CREDIT_CARD".equalsIgnoreCase(acc.getType()) || "LOAN".equalsIgnoreCase(acc.getType())) {
                if (acc.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                    liabilities = liabilities.add(acc.getBalance().abs());
                }
            } else {
                if (acc.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                    assets = assets.add(acc.getBalance());
                } else {
                    liabilities = liabilities.add(acc.getBalance().abs());
                }
            }
        }
        BigDecimal netWorth = assets.subtract(liabilities);

        List<PersonalBudgetDto> budgets = getBudgets(user, ym.toString());
        List<PersonalBudgetDto> alerts = budgets.stream()
                .filter(b -> b.getPercentUsed() >= b.getAlertThreshold())
                .collect(Collectors.toList());

        List<PersonalTransaction> recent = transactionRepository.findByUserIdOrderByTransactionDateDescCreatedAtDesc(
                user.getId(), PageRequest.of(0, 5)
        );

        List<PersonalTransaction> manaProjections = transactionRepository.findByUserIdAndIsManaProjectionTrueOrderByTransactionDateDesc(user.getId());

        return PersonalDashboardSummaryDto.builder()
                .totalIncome(income)
                .totalExpenses(expense)
                .netSavings(savings)
                .savingsRate(savingsRate)
                .totalAssets(assets)
                .totalLiabilities(liabilities)
                .netWorth(netWorth)
                .month(ym.format(MONTH_FMT))
                .recentTransactions(recent.stream().map(this::toTransactionDto).collect(Collectors.toList()))
                .budgetAlerts(alerts)
                .manaProjections(manaProjections.stream().map(this::toTransactionDto).collect(Collectors.toList()))
                .build();
    }

    // ─── Budgets ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PersonalBudgetDto> getBudgets(AppUser user, String month) {
        String m = (month != null && !month.isBlank()) ? month : YearMonth.now().format(MONTH_FMT);
        YearMonth ym = YearMonth.parse(m);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<PersonalBudget> budgets = budgetRepository.findByUserIdAndMonthOrderByCreatedAtAsc(user.getId(), m);
        List<PersonalBudgetDto> dtos = new ArrayList<>();

        for (PersonalBudget b : budgets) {
            BigDecimal spent = transactionRepository.sumCategoryExpensesForPeriod(user.getId(), b.getCategoryId(), start, end);
            if (spent == null) spent = BigDecimal.ZERO;

            BigDecimal limit = b.getLimitAmount();
            BigDecimal remaining = limit.subtract(spent);
            int pct = limit.compareTo(BigDecimal.ZERO) > 0
                    ? spent.multiply(BigDecimal.valueOf(100)).divide(limit, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
            boolean isOverspent = spent.compareTo(limit) > 0;

            Optional<PersonalCategory> catOpt = categoryRepository.findById(b.getCategoryId());

            dtos.add(PersonalBudgetDto.builder()
                    .id(b.getId())
                    .categoryId(b.getCategoryId())
                    .categoryName(catOpt.map(PersonalCategory::getName).orElse("Category"))
                    .categoryIcon(catOpt.map(PersonalCategory::getIcon).orElse("pie-chart-outline"))
                    .categoryColor(catOpt.map(PersonalCategory::getColor).orElse("#3B82F6"))
                    .limitAmount(limit)
                    .spentAmount(spent)
                    .remainingAmount(remaining)
                    .percentUsed(pct)
                    .period(b.getPeriod())
                    .month(b.getMonth())
                    .alertThreshold(b.getAlertThreshold())
                    .isOverspent(isOverspent)
                    .build());
        }
        return dtos;
    }

    @Override
    @Transactional
    public PersonalBudgetDto createBudget(CreatePersonalBudgetDto dto, AppUser user) {
        String m = (dto.getMonth() != null && !dto.getMonth().isBlank()) ? dto.getMonth() : YearMonth.now().format(MONTH_FMT);
        String bId = "bgt-" + UUID.randomUUID().toString().substring(0, 8);

        PersonalBudget budget = PersonalBudget.builder()
                .id(bId)
                .user(user)
                .categoryId(dto.getCategoryId())
                .limitAmount(dto.getLimitAmount())
                .period(dto.getPeriod() != null ? dto.getPeriod() : "MONTHLY")
                .month(m)
                .alertThreshold(dto.getAlertThreshold() != null ? dto.getAlertThreshold() : 80)
                .build();

        budgetRepository.save(budget);
        return getBudgets(user, m).stream().filter(b -> b.getId().equals(bId)).findFirst().orElse(null);
    }

    // ─── Recurring ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PersonalRecurringDto> getRecurringTransactions(AppUser user) {
        List<PersonalRecurring> list = recurringRepository.findByUserIdOrderByNextDueDateAsc(user.getId());
        return list.stream().map(this::toRecurringDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PersonalRecurringDto createRecurring(CreatePersonalRecurringDto dto, AppUser user) {
        PersonalAccount acc = accountRepository.findByIdAndUserId(dto.getAccountId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + dto.getAccountId()));

        String id = "rec-" + UUID.randomUUID().toString().substring(0, 8);
        LocalDate nextDue = dto.getNextDueDate() != null ? LocalDate.parse(dto.getNextDueDate()) : LocalDate.now().plusMonths(1);

        PersonalRecurring r = PersonalRecurring.builder()
                .id(id)
                .user(user)
                .name(dto.getName())
                .type(dto.getType())
                .amount(dto.getAmount())
                .categoryId(dto.getCategoryId())
                .accountId(acc.getId())
                .frequency(dto.getFrequency() != null ? dto.getFrequency() : "MONTHLY")
                .nextDueDate(nextDue)
                .isActive(true)
                .build();

        return toRecurringDto(recurringRepository.save(r));
    }

    @Override
    @Transactional
    public PersonalRecurringDto toggleRecurring(String id, AppUser user) {
        PersonalRecurring r = recurringRepository.findById(id)
                .filter(rec -> rec.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Recurring rule not found: " + id));

        r.setIsActive(!Boolean.TRUE.equals(r.getIsActive()));
        return toRecurringDto(recurringRepository.save(r));
    }

    // ─── Bills ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PersonalBillDto> getBills(AppUser user) {
        return billRepository.findByUserIdOrderByDueDateAsc(user.getId())
                .stream().map(this::toBillDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markBillPaid(String id, AppUser user) {
        PersonalBill bill = billRepository.findById(id)
                .filter(b -> b.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + id));

        bill.setIsPaid(true);
        billRepository.save(bill);
    }

    // ─── Analytics & Reports ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PersonalReportPeriodDto getReport(AppUser user, String period) {
        LocalDate start;
        LocalDate end;
        LocalDate now = LocalDate.now();

        if ("last-month".equalsIgnoreCase(period)) {
            YearMonth prev = YearMonth.now().minusMonths(1);
            start = prev.atDay(1);
            end = prev.atEndOfMonth();
        } else if ("quarter".equalsIgnoreCase(period)) {
            start = now.minusMonths(3);
            end = now;
        } else if ("year".equalsIgnoreCase(period)) {
            start = now.withDayOfYear(1);
            end = now;
        } else {
            YearMonth cur = YearMonth.now();
            start = cur.atDay(1);
            end = cur.atEndOfMonth();
        }

        BigDecimal income = transactionRepository.sumIncomeForPeriod(user.getId(), start, end);
        if (income == null) income = BigDecimal.ZERO;
        BigDecimal expense = transactionRepository.sumExpensesForPeriod(user.getId(), start, end);
        if (expense == null) expense = BigDecimal.ZERO;
        BigDecimal savings = income.subtract(expense);

        List<PersonalReportPeriodDto.MonthlyBreakdownDto> monthlyBreakdown = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            LocalDate mStart = ym.atDay(1);
            LocalDate mEnd = ym.atEndOfMonth();
            BigDecimal mInc = transactionRepository.sumIncomeForPeriod(user.getId(), mStart, mEnd);
            BigDecimal mExp = transactionRepository.sumExpensesForPeriod(user.getId(), mStart, mEnd);
            monthlyBreakdown.add(PersonalReportPeriodDto.MonthlyBreakdownDto.builder()
                    .month(ym.format(MONTH_FMT))
                    .label(ym.format(DISPLAY_MONTH_FMT))
                    .income(mInc != null ? mInc : BigDecimal.ZERO)
                    .expenses(mExp != null ? mExp : BigDecimal.ZERO)
                    .build());
        }

        return PersonalReportPeriodDto.builder()
                .period(period)
                .label(period.replace("-", " ").toUpperCase())
                .totalIncome(income)
                .totalExpenses(expense)
                .netSavings(savings)
                .topCategories(Collections.emptyList())
                .monthlyBreakdown(monthlyBreakdown)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalTransactionDto> getManaProjections(AppUser user) {
        return transactionRepository.findByUserIdAndIsManaProjectionTrueOrderByTransactionDateDesc(user.getId())
                .stream().map(this::toTransactionDto).collect(Collectors.toList());
    }

    // ─── P3: Installments & Loans ───────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PersonalInstallmentDto> getInstallments(AppUser user) {
        return installmentRepository.findByUserOrderByCreatedAtDesc(user)
                .stream().map(this::toInstallmentDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PersonalInstallmentDto createInstallment(CreatePersonalInstallmentDto dto, AppUser user) {
        PersonalInstallment inst = PersonalInstallment.builder()
                .user(user)
                .name(dto.getName())
                .totalAmount(dto.getTotalAmount())
                .monthlyEmi(dto.getMonthlyEmi())
                .interestRate(dto.getInterestRate() != null ? dto.getInterestRate() : BigDecimal.ZERO)
                .totalTenorMonths(dto.getTotalTenorMonths())
                .remainingTenorMonths(dto.getRemainingTenorMonths() != null ? dto.getRemainingTenorMonths() : dto.getTotalTenorMonths())
                .startDate(dto.getStartDate())
                .nextDueDate(dto.getNextDueDate() != null ? dto.getNextDueDate() : dto.getStartDate().plusMonths(1))
                .accountId(dto.getAccountId())
                .categoryId(dto.getCategoryId())
                .isAutoDeduct(Boolean.TRUE.equals(dto.getIsAutoDeduct()))
                .status("ACTIVE")
                .build();

        return toInstallmentDto(installmentRepository.save(inst));
    }

    @Override
    @Transactional
    public PersonalInstallmentDto payInstallment(String id, BigDecimal amount, String accountId, AppUser user) {
        PersonalInstallment inst = installmentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found: " + id));

        BigDecimal payAmount = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) ? amount : inst.getMonthlyEmi();
        String targetAccId = (accountId != null && !accountId.isBlank()) ? accountId : inst.getAccountId();

        if (targetAccId != null) {
            CreatePersonalTransactionDto txnDto = CreatePersonalTransactionDto.builder()
                    .accountId(targetAccId)
                    .categoryId(inst.getCategoryId())
                    .type("EXPENSE")
                    .amount(payAmount)
                    .description("EMI Payment: " + inst.getName())
                    .notes("Installment repayment. Remaining tenors: " + (inst.getRemainingTenorMonths() - 1))
                    .date(LocalDate.now().toString())
                    .build();
            createTransaction(txnDto, user);
        }

        if (inst.getRemainingTenorMonths() > 0) {
            inst.setRemainingTenorMonths(inst.getRemainingTenorMonths() - 1);
        }
        if (inst.getRemainingTenorMonths() <= 0) {
            inst.setStatus("COMPLETED");
            inst.setNextDueDate(null);
        } else if (inst.getNextDueDate() != null) {
            inst.setNextDueDate(inst.getNextDueDate().plusMonths(1));
        }

        return toInstallmentDto(installmentRepository.save(inst));
    }

    @Override
    @Transactional
    public void deleteInstallment(String id, AppUser user) {
        PersonalInstallment inst = installmentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found: " + id));
        installmentRepository.delete(inst);
    }

    // ─── P3: Savings Goals ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PersonalGoalDto> getGoals(AppUser user) {
        return goalRepository.findByUserOrderByCreatedAtDesc(user)
                .stream().map(this::toGoalDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PersonalGoalDto createGoal(CreatePersonalGoalDto dto, AppUser user) {
        PersonalGoal goal = PersonalGoal.builder()
                .user(user)
                .name(dto.getName())
                .targetAmount(dto.getTargetAmount())
                .currentAmount(dto.getCurrentAmount() != null ? dto.getCurrentAmount() : BigDecimal.ZERO)
                .targetDate(dto.getTargetDate())
                .icon(dto.getIcon() != null ? dto.getIcon() : "flag")
                .color(dto.getColor() != null ? dto.getColor() : "#10B981")
                .categoryId(dto.getCategoryId())
                .notes(dto.getNotes())
                .isCompleted(false)
                .build();

        return toGoalDto(goalRepository.save(goal));
    }

    @Override
    @Transactional
    public PersonalGoalDto contributeToGoal(String id, GoalContributionDto dto, AppUser user) {
        PersonalGoal goal = goalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found: " + id));

        if (dto.getAccountId() != null) {
            CreatePersonalTransactionDto txnDto = CreatePersonalTransactionDto.builder()
                    .accountId(dto.getAccountId())
                    .categoryId(goal.getCategoryId())
                    .type("EXPENSE")
                    .amount(dto.getAmount())
                    .description("Goal Savings Deposit: " + goal.getName())
                    .notes(dto.getNotes() != null ? dto.getNotes() : "Saved toward goal")
                    .date(LocalDate.now().toString())
                    .build();
            createTransaction(txnDto, user);
        }

        goal.setCurrentAmount(goal.getCurrentAmount().add(dto.getAmount()));
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setIsCompleted(true);
        }

        return toGoalDto(goalRepository.save(goal));
    }

    @Override
    @Transactional
    public void deleteGoal(String id, AppUser user) {
        PersonalGoal goal = goalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found: " + id));
        goalRepository.delete(goal);
    }

    // ─── P3: Batch Transaction Import ───────────────────────────────────────────

    @Override
    @Transactional
    public BatchImportResultDto batchImportTransactions(BatchImportTransactionDto dto, AppUser user) {
        if (dto.getTransactions() == null || dto.getTransactions().isEmpty()) {
            return BatchImportResultDto.builder()
                    .importedCount(0)
                    .failedCount(0)
                    .importedTransactions(Collections.emptyList())
                    .build();
        }

        List<PersonalTransactionDto> imported = new ArrayList<>();
        int failed = 0;

        for (CreatePersonalTransactionDto txnDto : dto.getTransactions()) {
            try {
                imported.add(createTransaction(txnDto, user));
            } catch (Exception ex) {
                log.error("Failed to import transaction: {}", txnDto, ex);
                failed++;
            }
        }

        return BatchImportResultDto.builder()
                .importedCount(imported.size())
                .failedCount(failed)
                .importedTransactions(imported)
                .build();
    }

    // ─── P3: Auto-Recurring Processing Engine ───────────────────────────────────

    @Override
    @Transactional
    public int processDueRecurring(AppUser user) {
        LocalDate today = LocalDate.now();
        List<PersonalRecurring> dueList = recurringRepository
                .findByUserIdAndIsActiveTrueAndNextDueDateLessThanEqual(user.getId(), today);

        int count = 0;
        for (PersonalRecurring rec : dueList) {
            try {
                executeRecurringRule(rec, rec.getUser());
                count++;
            } catch (Exception ex) {
                log.error("Error processing recurring rule {}: {}", rec.getId(), ex.getMessage());
            }
        }
        return count;
    }

    @Override
    @Transactional
    public int processAllDueRecurring() {
        LocalDate today = LocalDate.now();
        List<PersonalRecurring> dueList = recurringRepository
                .findByIsActiveTrueAndNextDueDateLessThanEqual(today);

        log.info("Processing {} due recurring transactions across all users", dueList.size());
        int count = 0;
        for (PersonalRecurring rec : dueList) {
            try {
                executeRecurringRule(rec, rec.getUser());
                count++;
            } catch (Exception ex) {
                log.error("Error processing recurring rule {} for user {}: {}", rec.getId(), rec.getUser().getId(), ex.getMessage());
            }
        }
        return count;
    }

    private void executeRecurringRule(PersonalRecurring rec, AppUser user) {
        CreatePersonalTransactionDto txnDto = CreatePersonalTransactionDto.builder()
                .accountId(rec.getAccountId())
                .categoryId(rec.getCategoryId())
                .type(rec.getType())
                .amount(rec.getAmount())
                .description("Auto Recurring: " + rec.getName())
                .notes("Generated automatically by recurring scheduler")
                .date(rec.getNextDueDate().toString())
                .build();

        createTransaction(txnDto, user);

        // Advance next due date
        LocalDate next = rec.getNextDueDate();
        String freq = rec.getFrequency() != null ? rec.getFrequency().toUpperCase() : "MONTHLY";
        switch (freq) {
            case "DAILY" -> next = next.plusDays(1);
            case "WEEKLY" -> next = next.plusWeeks(1);
            case "YEARLY" -> next = next.plusYears(1);
            case "MONTHLY" -> next = next.plusMonths(1);
            default -> next = next.plusMonths(1);
        }
        rec.setNextDueDate(next);
        recurringRepository.save(rec);
    }

    
    @Override
    public CreatePersonalTransactionDto parseNaturalLanguageText(String text, AppUser user) {
        if (text == null || text.isBlank()) {
            return CreatePersonalTransactionDto.builder().type("EXPENSE").amount(BigDecimal.ZERO).description("").build();
        }

        String lower = text.toLowerCase();
        String type = "EXPENSE";
        if (lower.contains("income") || lower.contains("salary") || lower.contains("received") || lower.contains("earned") || lower.contains("credit")) {
            type = "INCOME";
        } else if (lower.contains("transfer") || lower.contains("moved to")) {
            type = "TRANSFER";
        }

        BigDecimal amount = BigDecimal.ZERO;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:₹|rs|inr)?\\s*([0-9]+(?:\\.[0-9]{1,2})?)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            try {
                amount = new BigDecimal(m.group(1));
            } catch (Exception ignored) {}
        }

        String description = text.trim();
        String detectedCategory = "cat-2"; // default Food & Groceries
        if (lower.contains("coffee") || lower.contains("starbucks") || lower.contains("food") || lower.contains("restaurant") || lower.contains("swiggy") || lower.contains("zomato") || lower.contains("dinner") || lower.contains("lunch")) {
            detectedCategory = "cat-2";
        } else if (lower.contains("maintenance") || lower.contains("society") || lower.contains("rent") || lower.contains("flat") || lower.contains("home")) {
            detectedCategory = "cat-1";
        } else if (lower.contains("petrol") || lower.contains("fuel") || lower.contains("uber") || lower.contains("ola") || lower.contains("cab") || lower.contains("electricity") || lower.contains("bill") || lower.contains("wifi")) {
            detectedCategory = "cat-3";
        } else if (lower.contains("salary") || lower.contains("bonus") || lower.contains("freelance")) {
            detectedCategory = "cat-4";
        }

        List<PersonalAccount> accounts = accountRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
        String detectedAcc = !accounts.isEmpty() ? accounts.get(0).getId() : "acc-1";
        for (PersonalAccount acc : accounts) {
            if (lower.contains(acc.getName().toLowerCase()) || (acc.getBankName() != null && lower.contains(acc.getBankName().toLowerCase()))) {
                detectedAcc = acc.getId();
                break;
            }
        }

        return CreatePersonalTransactionDto.builder()
                .type(type)
                .amount(amount)
                .description(description)
                .categoryId(detectedCategory)
                .accountId(detectedAcc)
                .date(LocalDate.now().toString())
                .build();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private PersonalAccountDto toAccountDto(PersonalAccount a) {
        return PersonalAccountDto.builder()
                .id(a.getId())
                .name(a.getName())
                .type(a.getType())
                .balance(a.getBalance())
                .creditLimit(a.getCreditLimit())
                .currency(a.getCurrency())
                .bankName(a.getBankName())
                .accountNumber(a.getAccountNumber())
                .billingDay(a.getBillingDay())
                .paymentDueDay(a.getPaymentDueDay())
                .color(a.getColor())
                .icon(a.getIcon())
                .isActive(Boolean.TRUE.equals(a.getIsActive()))
                .createdAt(a.getCreatedAt() != null ? a.getCreatedAt().toString() : null)
                .build();
    }

    private PersonalTransactionDto toTransactionDto(PersonalTransaction t) {
        return PersonalTransactionDto.builder()
                .id(t.getId())
                .type(t.getType())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .categoryId(t.getCategoryId())
                .categoryName(t.getCategoryName())
                .categoryIcon(t.getCategoryIcon())
                .categoryColor(t.getCategoryColor())
                .subcategoryName(t.getSubcategoryName())
                .accountId(t.getAccountId())
                .accountName(t.getAccountName())
                .toAccountId(t.getToAccountId())
                .toAccountName(t.getToAccountName())
                .description(t.getDescription())
                .notes(t.getNotes())
                .receiptUrl(t.getReceiptUrl())
                .tags(t.getTags())
                .splitDetails(t.getSplitDetails())
                .date(t.getTransactionDate() != null ? t.getTransactionDate().toString() : null)
                .isManaProjection(Boolean.TRUE.equals(t.getIsManaProjection()))
                .sourceModule(t.getSourceModule())
                .sourceType(t.getSourceType())
                .sourceId(t.getSourceId())
                .sourceLabel(t.getSourceLabel())
                .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().toString() : null)
                .build();
    }

    private PersonalRecurringDto toRecurringDto(PersonalRecurring r) {
        return PersonalRecurringDto.builder()
                .id(r.getId())
                .name(r.getName())
                .type(r.getType())
                .amount(r.getAmount())
                .categoryId(r.getCategoryId())
                .accountId(r.getAccountId())
                .frequency(r.getFrequency())
                .nextDueDate(r.getNextDueDate() != null ? r.getNextDueDate().toString() : null)
                .isActive(Boolean.TRUE.equals(r.getIsActive()))
                .build();
    }

    private PersonalBillDto toBillDto(PersonalBill b) {
        return PersonalBillDto.builder()
                .id(b.getId())
                .name(b.getName())
                .amount(b.getAmount())
                .dueDate(b.getDueDate() != null ? b.getDueDate().toString() : null)
                .categoryId(b.getCategoryId())
                .isPaid(Boolean.TRUE.equals(b.getIsPaid()))
                .reminderDaysBefore(b.getReminderDaysBefore() != null ? b.getReminderDaysBefore() : 3)
                .build();
    }

    private PersonalInstallmentDto toInstallmentDto(PersonalInstallment i) {
        int paidTenors = i.getTotalTenorMonths() - i.getRemainingTenorMonths();
        BigDecimal paidAmount = i.getMonthlyEmi().multiply(BigDecimal.valueOf(paidTenors));
        BigDecimal remainingAmount = i.getMonthlyEmi().multiply(BigDecimal.valueOf(i.getRemainingTenorMonths()));
        int pctPaid = i.getTotalTenorMonths() > 0 ? (paidTenors * 100) / i.getTotalTenorMonths() : 0;

        String accName = null;
        if (i.getAccountId() != null) {
            accName = accountRepository.findById(i.getAccountId()).map(PersonalAccount::getName).orElse(null);
        }

        String catName = null;
        if (i.getCategoryId() != null) {
            catName = categoryRepository.findById(i.getCategoryId()).map(PersonalCategory::getName).orElse(null);
        }

        return PersonalInstallmentDto.builder()
                .id(i.getId())
                .name(i.getName())
                .totalAmount(i.getTotalAmount())
                .monthlyEmi(i.getMonthlyEmi())
                .interestRate(i.getInterestRate())
                .totalTenorMonths(i.getTotalTenorMonths())
                .remainingTenorMonths(i.getRemainingTenorMonths())
                .paidAmount(paidAmount)
                .remainingAmount(remainingAmount)
                .percentPaid(pctPaid)
                .startDate(i.getStartDate())
                .nextDueDate(i.getNextDueDate())
                .accountId(i.getAccountId())
                .accountName(accName)
                .categoryId(i.getCategoryId())
                .categoryName(catName)
                .isAutoDeduct(Boolean.TRUE.equals(i.getIsAutoDeduct()))
                .status(i.getStatus())
                .build();
    }

    private PersonalGoalDto toGoalDto(PersonalGoal g) {
        BigDecimal target = g.getTargetAmount();
        BigDecimal current = g.getCurrentAmount() != null ? g.getCurrentAmount() : BigDecimal.ZERO;
        BigDecimal remaining = target.subtract(current).max(BigDecimal.ZERO);
        int pctAchieved = target.compareTo(BigDecimal.ZERO) > 0
                ? current.multiply(BigDecimal.valueOf(100)).divide(target, 0, RoundingMode.HALF_UP).intValue()
                : 0;

        int monthsRemaining = 1;
        BigDecimal reqMonthly = remaining;
        if (g.getTargetDate() != null) {
            LocalDate now = LocalDate.now();
            if (g.getTargetDate().isAfter(now)) {
                Period p = Period.between(now, g.getTargetDate());
                monthsRemaining = Math.max(1, p.getYears() * 12 + p.getMonths());
                reqMonthly = remaining.divide(BigDecimal.valueOf(monthsRemaining), 2, RoundingMode.HALF_UP);
            }
        }

        return PersonalGoalDto.builder()
                .id(g.getId())
                .name(g.getName())
                .targetAmount(target)
                .currentAmount(current)
                .remainingAmount(remaining)
                .percentAchieved(pctAchieved)
                .requiredMonthlySavings(reqMonthly)
                .monthsRemaining(monthsRemaining)
                .targetDate(g.getTargetDate())
                .icon(g.getIcon())
                .color(g.getColor())
                .categoryId(g.getCategoryId())
                .notes(g.getNotes())
                .isCompleted(Boolean.TRUE.equals(g.getIsCompleted()))
                .build();
    }
}
