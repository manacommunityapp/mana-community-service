package com.manacommunity.api.finance.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.finance.dto.*;
import com.manacommunity.api.finance.entity.*;
import com.manacommunity.api.finance.repository.*;
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
public class SocietyFinanceService {

    private final SocietyExpenseRepository expenseRepo;
    private final ChartOfAccountRepository coaRepo;
    private final GeneralLedgerEntryRepository ledgerRepo;

    // ── Dashboard Summary ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public SocietyDashboardSummaryDto getDashboardSummary(Long communityId) {
        BigDecimal assetBalance = coaRepo.sumBalanceByType(communityId, ChartOfAccount.AccountType.ASSET);
        BigDecimal reserves = coaRepo.sumReserveFundBalances(communityId);
        long pendingApprovals = expenseRepo.countByCommunityIdAndStatusIn(communityId,
                List.of(SocietyExpense.ApprovalStatus.PENDING_CHECKER, SocietyExpense.ApprovalStatus.PENDING_APPROVER));
        long recentTxns = ledgerRepo.findByCommunityIdOrderByEntryDateDesc(communityId).stream()
                .filter(e -> e.getEntryDate().isAfter(LocalDate.now().minusDays(30)))
                .count();

        return SocietyDashboardSummaryDto.builder()
                .operatingBalance(assetBalance)
                .sinkingFundBalance(BigDecimal.ZERO)
                .fixedDepositsBalance(BigDecimal.ZERO)
                .totalReserves(reserves)
                .totalMonthlyDemand(BigDecimal.ZERO)
                .totalCollected(BigDecimal.ZERO)
                .collectionRate(BigDecimal.ZERO)
                .outstandingReceivables(BigDecimal.ZERO)
                .pendingPayables(BigDecimal.ZERO)
                .pendingApprovalsCount(pendingApprovals)
                .recentTransactionsCount(recentTxns)
                .build();
    }

    // ── Expenses ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SocietyExpenseDto> getExpenses(Long communityId, String status) {
        List<SocietyExpense> list;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            list = expenseRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(
                    communityId, SocietyExpense.ApprovalStatus.valueOf(status));
        } else {
            list = expenseRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        }
        return list.stream().map(this::toExpenseDto).toList();
    }

    @Transactional
    public SocietyExpenseDto createExpense(Long communityId, CreateSocietyExpenseDto dto, AppUser maker) {
        String voucherNumber = "VCH-" + LocalDate.now().getYear() + "-" +
                String.format("%04d", expenseRepo.findByCommunityIdOrderByCreatedAtDesc(communityId).size() + 1);

        SocietyExpense expense = SocietyExpense.builder()
                .communityId(communityId)
                .voucherNumber(voucherNumber)
                .category(dto.getCategory())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .accountId(dto.getAccountId())
                .accountName(resolveAccountName(communityId, dto.getAccountId()))
                .vendorId(dto.getVendorId())
                .receiptUrl(dto.getReceiptUrl())
                .status(SocietyExpense.ApprovalStatus.PENDING_CHECKER)
                .makerUserId(maker.getId())
                .makerName(maker.getFullName())
                .makerDate(LocalDate.now())
                .build();

        return toExpenseDto(expenseRepo.save(expense));
    }

    @Transactional
    public SocietyExpenseDto verifyExpense(Long id, String notes, AppUser checker) {
        SocietyExpense exp = requireExpense(id);
        exp.setStatus(SocietyExpense.ApprovalStatus.PENDING_APPROVER);
        exp.setCheckerUserId(checker.getId());
        exp.setCheckerName(checker.getFullName());
        exp.setCheckerDate(LocalDate.now());
        exp.setCheckerNotes(notes);
        return toExpenseDto(expenseRepo.save(exp));
    }

    @Transactional
    public SocietyExpenseDto approveExpense(Long id, String notes, AppUser approver) {
        SocietyExpense exp = requireExpense(id);
        exp.setStatus(SocietyExpense.ApprovalStatus.APPROVED);
        exp.setApproverUserId(approver.getId());
        exp.setApproverName(approver.getFullName());
        exp.setApproverDate(LocalDate.now());
        exp.setApproverNotes(notes);
        return toExpenseDto(expenseRepo.save(exp));
    }

    @Transactional
    public SocietyExpenseDto disburseExpense(Long id, String utr, String method) {
        SocietyExpense exp = requireExpense(id);
        exp.setStatus(SocietyExpense.ApprovalStatus.PAID);
        exp.setUtrReference(utr);
        exp.setPaymentMethod(method);
        return toExpenseDto(expenseRepo.save(exp));
    }

    // ── Chart of Accounts ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getChartOfAccounts(Long communityId) {
        return coaRepo.findByCommunityIdOrderByCode(communityId).stream()
                .map(a -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", a.getId());
                    m.put("code", a.getCode());
                    m.put("name", a.getName());
                    m.put("type", a.getType().name());
                    m.put("balance", a.getBalance());
                    m.put("description", a.getDescription());
                    m.put("isReserveFund", a.getIsReserveFund());
                    return m;
                }).toList();
    }

    // ── General Ledger ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLedgerEntries(Long communityId) {
        return ledgerRepo.findByCommunityIdOrderByEntryDateDesc(communityId).stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", e.getId());
                    m.put("entryDate", e.getEntryDate().toString());
                    m.put("voucherNumber", e.getVoucherNumber());
                    m.put("accountCode", e.getAccountCode());
                    m.put("accountName", e.getAccountName());
                    m.put("debit", e.getDebit());
                    m.put("credit", e.getCredit());
                    m.put("description", e.getDescription());
                    m.put("referenceType", e.getReferenceType());
                    m.put("referenceId", e.getReferenceId());
                    return m;
                }).toList();
    }

    // ── Reports ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getIncomeExpenseStatement(Long communityId, String year) {
        LocalDate fyStart = parseFyStart(year);
        LocalDate fyEnd = fyStart.plusYears(1).minusDays(1);
        LocalDate today = LocalDate.now();
        LocalDate effectiveEnd = today.isBefore(fyEnd) ? today : fyEnd;

        BigDecimal totalIncome = ledgerRepo.sumIncomeCredits(communityId, fyStart, effectiveEnd);
        BigDecimal totalExpenditure = ledgerRepo.sumExpenseDebits(communityId, fyStart, effectiveEnd);

        Map<String, Object> report = new HashMap<>();
        report.put("financialYear", year);
        report.put("period", fyStart.getMonth() + " " + fyStart.getYear() + " - " + effectiveEnd.getMonth() + " " + effectiveEnd.getYear());
        report.put("totalIncome", totalIncome);
        report.put("totalExpenditure", totalExpenditure);
        report.put("netSurplusDeficit", totalIncome.subtract(totalExpenditure));
        report.put("maintenanceCollections", BigDecimal.ZERO);
        report.put("amenityBookings", BigDecimal.ZERO);
        report.put("interestEarned", BigDecimal.ZERO);
        report.put("otherIncome", BigDecimal.ZERO);
        report.put("securityExpenses", BigDecimal.ZERO);
        report.put("housekeepingExpenses", BigDecimal.ZERO);
        report.put("electricityPowerExpenses", BigDecimal.ZERO);
        report.put("repairsMaintenanceExpenses", BigDecimal.ZERO);
        report.put("administrativeExpenses", BigDecimal.ZERO);
        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBalanceSheet(Long communityId) {
        BigDecimal assets = coaRepo.sumBalanceByType(communityId, ChartOfAccount.AccountType.ASSET);
        BigDecimal liabilities = coaRepo.sumBalanceByType(communityId, ChartOfAccount.AccountType.LIABILITY);
        BigDecimal equity = coaRepo.sumBalanceByType(communityId, ChartOfAccount.AccountType.EQUITY_RESERVE);

        Map<String, Object> bs = new HashMap<>();
        bs.put("asOfDate", LocalDate.now().toString());
        bs.put("totalAssets", assets);
        bs.put("operatingBankAccounts", BigDecimal.ZERO);
        bs.put("sinkingFundFixedDeposits", BigDecimal.ZERO);
        bs.put("memberMaintenanceReceivables", BigDecimal.ZERO);
        bs.put("securityDepositsWithUtilities", BigDecimal.ZERO);
        bs.put("totalLiabilitiesAndReserves", liabilities.add(equity));
        bs.put("sinkingFundReserve", BigDecimal.ZERO);
        bs.put("buildingRepairReserve", BigDecimal.ZERO);
        bs.put("generalReserveSurplus", equity);
        bs.put("vendorPayables", liabilities);
        bs.put("memberAdvanceCollections", BigDecimal.ZERO);
        return bs;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGstSummary(Long communityId, String month) {
        Map<String, Object> gst = new HashMap<>();
        gst.put("month", month);
        gst.put("outwardTaxableSupplies", BigDecimal.ZERO);
        gst.put("cgstCollected", BigDecimal.ZERO);
        gst.put("sgstCollected", BigDecimal.ZERO);
        gst.put("totalGstCollected", BigDecimal.ZERO);
        gst.put("inwardEligibleItc", BigDecimal.ZERO);
        gst.put("cgstItc", BigDecimal.ZERO);
        gst.put("sgstItc", BigDecimal.ZERO);
        gst.put("netGstPayable", BigDecimal.ZERO);
        gst.put("tdsDeductedTotal", BigDecimal.ZERO);
        return gst;
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private SocietyExpense requireExpense(Long id) {
        return expenseRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SocietyExpense", id));
    }

    private String resolveAccountName(Long communityId, String accountId) {
        if (accountId == null) return null;
        try {
            Long id = Long.valueOf(accountId);
            return coaRepo.findById(id).map(ChartOfAccount::getName).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseFyStart(String year) {
        if (year != null && year.contains("-")) {
            String startYear = year.split("-")[0].trim();
            return LocalDate.of(Integer.parseInt(startYear), 4, 1);
        }
        return LocalDate.of(LocalDate.now().getYear(), 4, 1);
    }

    private SocietyExpenseDto toExpenseDto(SocietyExpense e) {
        return SocietyExpenseDto.builder()
                .id(e.getId())
                .voucherNumber(e.getVoucherNumber())
                .category(e.getCategory())
                .title(e.getTitle())
                .description(e.getDescription())
                .amount(e.getAmount())
                .accountId(e.getAccountId())
                .accountName(e.getAccountName())
                .vendorId(e.getVendorId())
                .vendorName(e.getVendorName())
                .status(e.getStatus().name())
                .makerName(e.getMakerName())
                .makerDate(e.getMakerDate() != null ? e.getMakerDate().toString() : null)
                .checkerName(e.getCheckerName())
                .checkerDate(e.getCheckerDate() != null ? e.getCheckerDate().toString() : null)
                .checkerNotes(e.getCheckerNotes())
                .approverName(e.getApproverName())
                .approverDate(e.getApproverDate() != null ? e.getApproverDate().toString() : null)
                .approverNotes(e.getApproverNotes())
                .receiptUrl(e.getReceiptUrl())
                .paymentMethod(e.getPaymentMethod())
                .utrReference(e.getUtrReference())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .build();
    }
}
