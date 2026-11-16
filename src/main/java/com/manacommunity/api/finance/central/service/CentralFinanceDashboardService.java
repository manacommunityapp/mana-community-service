package com.manacommunity.api.finance.central.service;

import com.manacommunity.api.cfbos.dashboard.service.CommunityFinanceDashboardService;
import com.manacommunity.api.cfbos.dashboard.dto.CommunityFinanceSummaryResponse;
import com.manacommunity.api.finance.central.dto.CentralFinanceSummaryResponse;
import com.manacommunity.api.finance.entity.SocietyExpense;
import com.manacommunity.api.finance.repository.FinanceInvoiceRepository;
import com.manacommunity.api.finance.repository.SocietyExpenseRepository;
import com.manacommunity.api.transactioncore.enums.TransactionDomain;
import com.manacommunity.api.transactioncore.enums.TransactionIntentStatus;
import com.manacommunity.api.transactioncore.repository.TransactionIntentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CentralFinanceDashboardService {

    private final CommunityFinanceDashboardService cfbosDashboardService;
    private final TransactionIntentRepository intentRepository;
    private final SocietyExpenseRepository societyExpenseRepository;
    private final FinanceInvoiceRepository financeInvoiceRepository;

    private static final List<TransactionIntentStatus> CAPTURED_STATUSES =
            List.of(TransactionIntentStatus.CAPTURED, TransactionIntentStatus.SETTLED, TransactionIntentStatus.ESCROW_HELD);

    private static final List<TransactionIntentStatus> SETTLED_STATUSES =
            List.of(TransactionIntentStatus.CAPTURED, TransactionIntentStatus.SETTLED);

    @Transactional(readOnly = true)
    public CentralFinanceSummaryResponse getSummary(Long communityId) {
        log.info("Building central finance dashboard for communityId={}", communityId);

        // ── CFBOS billing module ───────────────────────────────────────────────
        CommunityFinanceSummaryResponse cfbos = cfbosDashboardService.getSummary(communityId);

        // ── TransactionCore: total collected across all domains ───────────────
        BigDecimal txcTotalCollected = safeSum(
                intentRepository.sumAmountByCommunityIdAndStatusIn(communityId, SETTLED_STATUSES));

        // ── TransactionCore: by domain ────────────────────────────────────────
        BigDecimal sportsCollected = safeSum(
                intentRepository.sumAmountByCommunityIdAndDomainAndStatusIn(communityId, TransactionDomain.SPORTS, SETTLED_STATUSES));
        BigDecimal sportsEscrow = safeSum(
                intentRepository.sumEscrowHeldByCommunityIdAndStatus(communityId, TransactionIntentStatus.ESCROW_HELD));
        long sportsEscrowCount = intentRepository.countEscrowHeldByCommunityIdAndStatus(communityId, TransactionIntentStatus.ESCROW_HELD);

        BigDecimal eventsCollected = safeSum(
                intentRepository.sumAmountByCommunityIdAndDomainAndStatusIn(communityId, TransactionDomain.EVENTS, SETTLED_STATUSES));
        BigDecimal eventsEscrow = safeSum(
                intentRepository.sumAmountByCommunityIdAndDomainAndStatusIn(communityId, TransactionDomain.EVENTS,
                        List.of(TransactionIntentStatus.ESCROW_HELD)));

        BigDecimal marketplaceCollected = safeSum(
                intentRepository.sumAmountByCommunityIdAndDomainAndStatusIn(communityId, TransactionDomain.MARKETPLACE, SETTLED_STATUSES));

        BigDecimal resourceBookingCollected = safeSum(
                intentRepository.sumAmountByCommunityIdAndDomainAndStatusIn(communityId, TransactionDomain.RESOURCE_BOOKING, SETTLED_STATUSES));

        // ── Society expenses (community-scoped) ───────────────────────────────
        BigDecimal societyApproved = safeSum(
                societyExpenseRepository.sumAmountByCommunityIdAndStatus(communityId, SocietyExpense.ApprovalStatus.PAID));
        long societyPending = societyExpenseRepository.countByCommunityIdAndStatusIn(communityId,
                List.of(SocietyExpense.ApprovalStatus.PENDING_MAKER,
                        SocietyExpense.ApprovalStatus.PENDING_CHECKER,
                        SocietyExpense.ApprovalStatus.PENDING_APPROVER));

        // ── Finance module invoices (global totals) ───────────────────────────
        BigDecimal financeOutstanding = safeSum(financeInvoiceRepository.sumOutstandingReceivables());
        long financeInvoiceCount = financeInvoiceRepository.count();

        // ── Cross-module rollups ───────────────────────────────────────────────
        BigDecimal maintenanceCollected = cfbos.getTotalCollected() != null ? cfbos.getTotalCollected() : BigDecimal.ZERO;
        BigDecimal totalCollected = maintenanceCollected.add(txcTotalCollected);

        BigDecimal maintenanceOutstanding = cfbos.getTotalOutstanding() != null ? cfbos.getTotalOutstanding() : BigDecimal.ZERO;
        BigDecimal totalOutstanding = maintenanceOutstanding.add(financeOutstanding);

        BigDecimal cfbosExpenses = cfbos.getTotalExpenses() != null ? cfbos.getTotalExpenses() : BigDecimal.ZERO;
        BigDecimal totalExpenses = cfbosExpenses.add(societyApproved);

        BigDecimal totalEscrowHeld = sportsEscrow.add(eventsEscrow);
        BigDecimal netPosition = totalCollected.subtract(totalExpenses);

        return CentralFinanceSummaryResponse.builder()
                .communityId(communityId)
                // Cross-module rollups
                .totalCollected(scale2(totalCollected))
                .totalOutstanding(scale2(totalOutstanding))
                .totalExpenses(scale2(totalExpenses))
                .totalEscrowHeld(scale2(totalEscrowHeld))
                .netPosition(scale2(netPosition))
                // CFBOS maintenance billing
                .maintenanceTotalBilled(cfbos.getTotalBilled() != null ? scale2(cfbos.getTotalBilled()) : BigDecimal.ZERO)
                .maintenanceTotalCollected(scale2(maintenanceCollected))
                .maintenanceTotalOutstanding(scale2(maintenanceOutstanding))
                .maintenanceTotalOverdue(cfbos.getTotalOverdue() != null ? scale2(cfbos.getTotalOverdue()) : BigDecimal.ZERO)
                .maintenancePendingInvoices(cfbos.getPendingInvoices())
                .maintenanceOverdueInvoices(cfbos.getOverdueInvoices())
                // Sports module
                .sportsTotalCollected(scale2(sportsCollected))
                .sportsEscrowHeld(scale2(sportsEscrow))
                .sportsEscrowPendingCount(sportsEscrowCount)
                // Events module
                .eventsTotalCollected(scale2(eventsCollected))
                .eventsEscrowHeld(scale2(eventsEscrow))
                // Marketplace
                .marketplaceTotalCollected(scale2(marketplaceCollected))
                // Resource booking
                .resourceBookingTotalCollected(scale2(resourceBookingCollected))
                // Society expenses
                .societyApprovedExpenses(scale2(societyApproved))
                .societyPendingApprovals(societyPending)
                // Finance module invoices
                .financeInvoicesOutstanding(scale2(financeOutstanding))
                .financeInvoiceCount(financeInvoiceCount)
                .build();
    }

    private BigDecimal safeSum(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal scale2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
