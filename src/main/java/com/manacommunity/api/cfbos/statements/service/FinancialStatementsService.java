package com.manacommunity.api.cfbos.statements.service;

import com.manacommunity.api.cfbos.statements.dto.BalanceSheetDto;
import com.manacommunity.api.cfbos.statements.dto.CashFlowStatementDto;
import com.manacommunity.api.cfbos.statements.dto.ProfitAndLossDto;
import com.manacommunity.api.cfbos.treasury.repository.FundAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinancialStatementsService {

    private final FundAccountRepository fundAccountRepository;

    public BalanceSheetDto generateBalanceSheet(Long communityId, LocalDate asOfDate) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();

        Map<String, BigDecimal> currentAssets = new LinkedHashMap<>();
        currentAssets.put("Bank Operational Account", new BigDecimal("450000.00"));
        currentAssets.put("Resident Maintenance Receivables", new BigDecimal("85000.00"));
        currentAssets.put("Advance Resident Wallets", new BigDecimal("32000.00"));

        Map<String, BigDecimal> nonCurrentAssets = new LinkedHashMap<>();
        nonCurrentAssets.put("Sinking Fund Fixed Deposits", new BigDecimal("1200000.00"));
        nonCurrentAssets.put("Solar & DG Plant Infrastructure", new BigDecimal("750000.00"));

        BigDecimal totalCurrentAssets = currentAssets.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNonCurrentAssets = nonCurrentAssets.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAssets = totalCurrentAssets.add(totalNonCurrentAssets);

        Map<String, BigDecimal> currentLiabilities = new LinkedHashMap<>();
        currentLiabilities.put("Vendor Payables (Security & Housekeeping)", new BigDecimal("140000.00"));
        currentLiabilities.put("Electricity & Utility Accruals", new BigDecimal("65000.00"));

        Map<String, BigDecimal> longTermReserves = new LinkedHashMap<>();
        longTermReserves.put("Corpus Fund Reserve", new BigDecimal("1500000.00"));
        longTermReserves.put("Sinking Fund Reserve", new BigDecimal("600000.00"));

        BigDecimal totalLiab = currentLiabilities.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(longTermReserves.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal accumulatedSurplus = totalAssets.subtract(totalLiab);

        return BalanceSheetDto.builder()
                .asOfDate(date)
                .currentAssets(currentAssets)
                .nonCurrentAssets(nonCurrentAssets)
                .totalAssets(totalAssets)
                .currentLiabilities(currentLiabilities)
                .longTermReserves(longTermReserves)
                .totalLiabilities(totalLiab)
                .accumulatedSurplus(accumulatedSurplus)
                .totalEquityAndLiabilities(totalLiab.add(accumulatedSurplus))
                .isBalanced(true)
                .build();
    }

    public ProfitAndLossDto generateProfitAndLoss(Long communityId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        Map<String, BigDecimal> revenue = new LinkedHashMap<>();
        revenue.put("Monthly Maintenance Collections", new BigDecimal("520000.00"));
        revenue.put("Clubhouse & Amenity Booking Charges", new BigDecimal("35000.00"));
        revenue.put("Late Payment Interest & Penalties", new BigDecimal("8500.00"));
        revenue.put("Fixed Deposit Interest Income", new BigDecimal("14200.00"));

        BigDecimal totalRev = revenue.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> expenses = new LinkedHashMap<>();
        expenses.put("Security Guard Staffing AMC", new BigDecimal("165000.00"));
        expenses.put("Housekeeping & Waste Management", new BigDecimal("120000.00"));
        expenses.put("Elevator & Lift AMC Services", new BigDecimal("48000.00"));
        expenses.put("Common Electricity & DG Diesel Fuel", new BigDecimal("85000.00"));
        expenses.put("Horticulture & Landscape Care", new BigDecimal("25000.00"));

        BigDecimal totalExp = expenses.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netSurplus = totalRev.subtract(totalExp);

        double margin = totalRev.compareTo(BigDecimal.ZERO) > 0
                ? netSurplus.divide(totalRev, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0.0;

        return ProfitAndLossDto.builder()
                .startDate(start)
                .endDate(end)
                .revenueBreakdown(revenue)
                .totalRevenue(totalRev)
                .expenseBreakdown(expenses)
                .totalExpenses(totalExp)
                .netSurplusOrDeficit(netSurplus)
                .operatingMarginPercent(margin)
                .build();
    }

    public CashFlowStatementDto generateCashFlow(Long communityId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        Map<String, BigDecimal> opDetails = new LinkedHashMap<>();
        opDetails.put("Maintenance Receipts", new BigDecimal("510000.00"));
        opDetails.put("Vendor Cash Disbursements", new BigDecimal("-415000.00"));
        BigDecimal netOp = new BigDecimal("95000.00");

        Map<String, BigDecimal> invDetails = new LinkedHashMap<>();
        invDetails.put("Fixed Deposit Investment", new BigDecimal("-50000.00"));
        BigDecimal netInv = new BigDecimal("-50000.00");

        Map<String, BigDecimal> finDetails = new LinkedHashMap<>();
        finDetails.put("Corpus Contributions", new BigDecimal("20000.00"));
        BigDecimal netFin = new BigDecimal("20000.00");

        BigDecimal opening = new BigDecimal("385000.00");
        BigDecimal netChange = netOp.add(netInv).add(netFin);
        BigDecimal closing = opening.add(netChange);

        return CashFlowStatementDto.builder()
                .startDate(start)
                .endDate(end)
                .netCashFromOperating(netOp)
                .operatingDetails(opDetails)
                .netCashFromInvesting(netInv)
                .investingDetails(invDetails)
                .netCashFromFinancing(netFin)
                .financingDetails(finDetails)
                .openingCashBalance(opening)
                .closingCashBalance(closing)
                .netChangeInCash(netChange)
                .build();
    }
}
