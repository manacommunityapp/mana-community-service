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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SocietyFinanceService {

    private final SocietyExpenseRepository expenseRepo;
    private final SocietyVendorBillRepository vendorBillRepo;
    private final FinanceInvoiceRepository invoiceRepo;
    private final ChartOfAccountRepository coaRepo;
    private final GeneralLedgerEntryRepository ledgerRepo;

    // ── Dashboard Summary ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public SocietyDashboardSummaryDto getDashboardSummary(Long communityId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        BigDecimal assetBalance = coaRepo.sumBalanceByType(communityId, ChartOfAccount.AccountType.ASSET);
        BigDecimal sinkingFund = coaRepo.sumReserveFundBalances(communityId);
        BigDecimal fixedDeposits = coaRepo.sumFixedDepositBalances(communityId);
        BigDecimal totalCollected = ledgerRepo.sumMonthlyCollections(communityId, monthStart, today);
        BigDecimal outstandingReceivables = invoiceRepo.sumOutstandingReceivables();
        BigDecimal pendingPayables = vendorBillRepo.sumPendingPayables(communityId,
                List.of(SocietyVendorBill.BillStatus.PAID, SocietyVendorBill.BillStatus.DRAFT));

        BigDecimal totalDemand = totalCollected.add(outstandingReceivables);
        BigDecimal collectionRate = totalDemand.compareTo(BigDecimal.ZERO) > 0
                ? totalCollected.multiply(BigDecimal.valueOf(100))
                        .divide(totalDemand, 1, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(100);

        long pendingApprovals = expenseRepo.countByCommunityIdAndStatusIn(communityId,
                List.of(SocietyExpense.ApprovalStatus.PENDING_CHECKER, SocietyExpense.ApprovalStatus.PENDING_APPROVER));
        long recentTxns = ledgerRepo.findByCommunityIdOrderByEntryDateDesc(communityId).stream()
                .filter(e -> e.getEntryDate().isAfter(today.minusDays(30)))
                .count();

        return SocietyDashboardSummaryDto.builder()
                .operatingBalance(assetBalance)
                .sinkingFundBalance(sinkingFund)
                .fixedDepositsBalance(fixedDeposits)
                .totalReserves(sinkingFund.add(fixedDeposits))
                .totalMonthlyDemand(totalDemand)
                .totalCollected(totalCollected)
                .collectionRate(collectionRate)
                .outstandingReceivables(outstandingReceivables)
                .pendingPayables(pendingPayables)
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

    // ── Vendor Bills ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SocietyVendorBillDto> getVendorBills(Long communityId, String status) {
        List<SocietyVendorBill> bills;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            bills = vendorBillRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(
                    communityId, SocietyVendorBill.BillStatus.valueOf(status));
        } else {
            bills = vendorBillRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        }
        return bills.stream().map(this::toBillDto).toList();
    }

    @Transactional
    public SocietyVendorBillDto createVendorBill(Long communityId, CreateSocietyVendorBillDto dto, AppUser creator) {
        BigDecimal gross = dto.getGrossAmount();
        BigDecimal gstPct = dto.getGstPercent() != null ? dto.getGstPercent() : BigDecimal.ZERO;
        BigDecimal gstAmt = gross.multiply(gstPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal tdsRate = dto.getTdsRate() != null ? dto.getTdsRate() : BigDecimal.ZERO;
        BigDecimal tdsAmt = gross.multiply(tdsRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal net = gross.add(gstAmt).subtract(tdsAmt);

        String billNo = dto.getBillNumber() != null ? dto.getBillNumber()
                : "BILL-" + LocalDate.now().getYear() + "-"
                  + String.format("%04d", vendorBillRepo.findByCommunityIdOrderByCreatedAtDesc(communityId).size() + 1);

        SocietyVendorBill bill = SocietyVendorBill.builder()
                .communityId(communityId)
                .billNumber(billNo)
                .vendorId(dto.getVendorId())
                .vendorName(dto.getVendorName())
                .billDate(dto.getBillDate() != null ? dto.getBillDate() : LocalDate.now())
                .dueDate(dto.getDueDate())
                .description(dto.getDescription())
                .grossAmount(gross)
                .gstPercent(gstPct)
                .gstAmount(gstAmt)
                .tdsSection(dto.getTdsSection())
                .tdsAmount(tdsAmt)
                .netPayable(net)
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(net)
                .status(SocietyVendorBill.BillStatus.RECEIVED)
                .createdByName(creator.getFullName())
                .build();

        return toBillDto(vendorBillRepo.save(bill));
    }

    @Transactional
    public SocietyVendorBillDto approveVendorBill(Long id, String notes, AppUser approver) {
        SocietyVendorBill bill = requireBill(id);
        bill.setStatus(SocietyVendorBill.BillStatus.APPROVED);
        bill.setApprovedByName(approver.getFullName());
        bill.setApprovedDate(LocalDate.now());
        bill.setApprovalNotes(notes);
        return toBillDto(vendorBillRepo.save(bill));
    }

    @Transactional
    public SocietyVendorBillDto payVendorBill(Long id, PaySocietyVendorBillDto dto) {
        SocietyVendorBill bill = requireBill(id);
        BigDecimal newPaid = bill.getPaidAmount().add(dto.getAmount());
        bill.setPaidAmount(newPaid);
        BigDecimal newBalance = bill.getNetPayable().subtract(newPaid);
        bill.setBalanceDue(newBalance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newBalance);
        bill.setStatus(bill.getBalanceDue().compareTo(BigDecimal.ZERO) == 0
                ? SocietyVendorBill.BillStatus.PAID
                : SocietyVendorBill.BillStatus.PARTIALLY_PAID);
        bill.setPaymentMethod(dto.getPaymentMethod());
        bill.setUtrReference(dto.getUtrReference());
        bill.setPaidDate(LocalDate.now());

        String voucherNo = "VBP-" + System.currentTimeMillis();
        bill.setGlVoucherNumber(voucherNo);
        bill = vendorBillRepo.save(bill);

        // GL: Dr Vendor Payables (2030) / Cr Operating Bank (1010)
        postGl(bill.getCommunityId(), voucherNo, "2030", "Vendor Payables",
                dto.getAmount(), BigDecimal.ZERO, "VENDOR_BILL", String.valueOf(id));
        postGl(bill.getCommunityId(), voucherNo, "1010", "Operating Bank",
                BigDecimal.ZERO, dto.getAmount(), "VENDOR_BILL", String.valueOf(id));

        return toBillDto(bill);
    }

    // ── Invoice Payment Recording ──────────────────────────────────

    @Transactional
    public Map<String, Object> recordInvoicePayment(Long communityId, RecordInvoicePaymentDto dto) {
        FinanceInvoice invoice = invoiceRepo.findById(dto.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("FinanceInvoice", dto.getInvoiceId()));

        invoice.setStatus("Paid");
        invoiceRepo.save(invoice);

        String voucherNo = "RCP-" + System.currentTimeMillis();

        // GL: Dr Operating Bank (1010) / Cr Maintenance Collections (3010)
        postGl(communityId, voucherNo, "1010", "Operating Bank",
                dto.getAmount(), BigDecimal.ZERO, "INVOICE_PAYMENT", String.valueOf(dto.getInvoiceId()));
        postGl(communityId, voucherNo, "3010", "Maintenance Collections",
                BigDecimal.ZERO, dto.getAmount(), "INVOICE_PAYMENT", String.valueOf(dto.getInvoiceId()));

        Map<String, Object> result = new HashMap<>();
        result.put("voucherNumber", voucherNo);
        result.put("invoiceId", dto.getInvoiceId());
        result.put("amount", dto.getAmount());
        result.put("status", "Paid");
        return result;
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private SocietyVendorBill requireBill(Long id) {
        return vendorBillRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SocietyVendorBill", id));
    }

    private void postGl(Long communityId, String voucherNo, String accountCode, String accountName,
                        BigDecimal debit, BigDecimal credit, String refType, String refId) {
        ledgerRepo.save(GeneralLedgerEntry.builder()
                .communityId(communityId)
                .entryDate(LocalDate.now())
                .voucherNumber(voucherNo)
                .accountCode(accountCode)
                .accountName(accountName)
                .debit(debit)
                .credit(credit)
                .referenceType(refType)
                .referenceId(refId)
                .build());
    }

    private SocietyVendorBillDto toBillDto(SocietyVendorBill b) {
        return SocietyVendorBillDto.builder()
                .id(b.getId())
                .billNumber(b.getBillNumber())
                .vendorId(b.getVendorId())
                .vendorName(b.getVendorName())
                .billDate(b.getBillDate() != null ? b.getBillDate().toString() : null)
                .dueDate(b.getDueDate() != null ? b.getDueDate().toString() : null)
                .description(b.getDescription())
                .grossAmount(b.getGrossAmount())
                .gstPercent(b.getGstPercent())
                .gstAmount(b.getGstAmount())
                .tdsSection(b.getTdsSection())
                .tdsAmount(b.getTdsAmount())
                .netPayable(b.getNetPayable())
                .paidAmount(b.getPaidAmount())
                .balanceDue(b.getBalanceDue())
                .status(b.getStatus().name())
                .approvedByName(b.getApprovedByName())
                .approvedDate(b.getApprovedDate() != null ? b.getApprovedDate().toString() : null)
                .approvalNotes(b.getApprovalNotes())
                .paymentMethod(b.getPaymentMethod())
                .utrReference(b.getUtrReference())
                .paidDate(b.getPaidDate() != null ? b.getPaidDate().toString() : null)
                .glVoucherNumber(b.getGlVoucherNumber())
                .createdByName(b.getCreatedByName())
                .createdAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : null)
                .build();
    }

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
