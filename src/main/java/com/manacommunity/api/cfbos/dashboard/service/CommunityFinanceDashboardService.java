package com.manacommunity.api.cfbos.dashboard.service;

import com.manacommunity.api.cfbos.dashboard.dto.CommunityFinanceSummaryResponse;
import com.manacommunity.api.cfbos.expense.entity.CfbosExpense;
import com.manacommunity.api.cfbos.expense.enums.ExpenseStatus;
import com.manacommunity.api.cfbos.expense.repository.CfbosExpenseRepository;
import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoice;
import com.manacommunity.api.cfbos.invoice.enums.InvoiceStatus;
import com.manacommunity.api.cfbos.invoice.repository.CfbosInvoiceRepository;
import com.manacommunity.api.cfbos.payment.entity.CfbosPayment;
import com.manacommunity.api.cfbos.payment.repository.CfbosPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityFinanceDashboardService {

    private final CfbosInvoiceRepository invoiceRepository;
    private final CfbosExpenseRepository expenseRepository;
    private final CfbosPaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public CommunityFinanceSummaryResponse getSummary(Long communityId) {
        // Invoices scoped by community_id (backfilled via V100/V101 migrations)
        List<CfbosInvoice> invoices = invoiceRepository.findByCommunityId(communityId);
        List<CfbosExpense> expenses = expenseRepository.findByCommunityId(communityId);
        List<CfbosPayment> payments = paymentRepository.findByPropertyIdIn(
                invoices.stream()
                        .filter(i -> i.getPropertyId() != null)
                        .map(CfbosInvoice::getPropertyId)
                        .distinct()
                        .toList()
        );

        BigDecimal totalBilled = invoices.stream()
                .map(CfbosInvoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCollected = invoices.stream()
                .map(CfbosInvoice::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstanding = invoices.stream()
                .map(CfbosInvoice::getBalanceDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();
        BigDecimal totalOverdue = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.OVERDUE
                        || (i.getBalanceDue().compareTo(BigDecimal.ZERO) > 0 && i.getDueDate().isBefore(today)))
                .map(CfbosInvoice::getBalanceDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long overdueCount = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.OVERDUE
                        || (i.getBalanceDue().compareTo(BigDecimal.ZERO) > 0 && i.getDueDate().isBefore(today)))
                .count();

        int collectionPct = totalBilled.compareTo(BigDecimal.ZERO) > 0
                ? totalCollected.multiply(BigDecimal.valueOf(100))
                        .divide(totalBilled, 0, RoundingMode.HALF_UP)
                        .intValue()
                : 0;

        BigDecimal totalExpenses = expenses.stream()
                .map(CfbosExpense::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal approvedExpenses = expenses.stream()
                .filter(e -> e.getStatus() == ExpenseStatus.APPROVED || e.getStatus() == ExpenseStatus.PAID)
                .map(CfbosExpense::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingExpenses = expenses.stream()
                .filter(e -> e.getStatus() == ExpenseStatus.DRAFT || e.getStatus() == ExpenseStatus.PENDING_APPROVAL)
                .map(CfbosExpense::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaymentsAmount = payments.stream()
                .map(CfbosPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Aging: 0-30 current, 31-60, 61-90, 90+
        BigDecimal current = overdueBucket(invoices, today, 0, 30);
        BigDecimal bucket31 = overdueBucket(invoices, today, 31, 60);
        BigDecimal bucket61 = overdueBucket(invoices, today, 61, 90);
        BigDecimal bucket90 = overdueBucket(invoices, today, 91, Integer.MAX_VALUE);

        return CommunityFinanceSummaryResponse.builder()
                .communityId(communityId)
                .totalBilled(totalBilled)
                .totalCollected(totalCollected)
                .totalOutstanding(totalOutstanding)
                .totalOverdue(totalOverdue)
                .collectionPercent(collectionPct)
                .totalInvoices(invoices.size())
                .paidInvoices(invoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).count())
                .overdueInvoices(overdueCount)
                .pendingInvoices(invoices.stream().filter(i -> i.getStatus() == InvoiceStatus.ISSUED
                        || i.getStatus() == InvoiceStatus.DRAFT).count())
                .totalExpenses(totalExpenses)
                .approvedExpenses(approvedExpenses)
                .pendingExpenses(pendingExpenses)
                .totalPayments(payments.size())
                .totalPaymentsAmount(totalPaymentsAmount)
                .agingBuckets(List.of(
                        bucket("0 - 30 Days (Current)", current, countBucket(invoices, today, 0, 30),
                                totalOutstanding),
                        bucket("31 - 60 Days", bucket31, countBucket(invoices, today, 31, 60),
                                totalOutstanding),
                        bucket("61 - 90 Days", bucket61, countBucket(invoices, today, 61, 90),
                                totalOutstanding),
                        bucket("90+ Days (Defaulters)", bucket90, countBucket(invoices, today, 91, Integer.MAX_VALUE),
                                totalOutstanding)
                ))
                .build();
    }

    private BigDecimal overdueBucket(List<CfbosInvoice> invoices, LocalDate today, int fromDays, int toDays) {
        return invoices.stream()
                .filter(i -> i.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
                .filter(i -> {
                    long overdueDays = today.toEpochDay() - i.getDueDate().toEpochDay();
                    return overdueDays >= fromDays && (toDays == Integer.MAX_VALUE || overdueDays <= toDays);
                })
                .map(CfbosInvoice::getBalanceDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countBucket(List<CfbosInvoice> invoices, LocalDate today, int fromDays, int toDays) {
        return invoices.stream()
                .filter(i -> i.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
                .filter(i -> {
                    long overdueDays = today.toEpochDay() - i.getDueDate().toEpochDay();
                    return overdueDays >= fromDays && (toDays == Integer.MAX_VALUE || overdueDays <= toDays);
                })
                .count();
    }

    private CommunityFinanceSummaryResponse.AgingBucket bucket(String period, BigDecimal amount, long count,
                                                                  BigDecimal total) {
        int pct = total.compareTo(BigDecimal.ZERO) > 0
                ? amount.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue()
                : 0;
        return CommunityFinanceSummaryResponse.AgingBucket.builder()
                .period(period).amount(amount).count(count).percentage(pct).build();
    }
}
