package com.manacommunity.api.finance.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CentralFinanceSummaryResponse {

    private Long communityId;

    // ── Cross-module rolled-up totals ─────────────────────────────────────────
    /** Total collected across all payment modules (billing + transactioncore). */
    private BigDecimal totalCollected;

    /** Total outstanding/pending across all billing + finance invoice modules. */
    private BigDecimal totalOutstanding;

    /** Total approved/paid expenses across CFBOS expense and society expenses. */
    private BigDecimal totalExpenses;

    /** Funds currently held in escrow across all modules. */
    private BigDecimal totalEscrowHeld;

    /** Net position: totalCollected minus totalExpenses. */
    private BigDecimal netPosition;

    // ── CFBOS maintenance billing ─────────────────────────────────────────────
    private BigDecimal maintenanceTotalBilled;
    private BigDecimal maintenanceTotalCollected;
    private BigDecimal maintenanceTotalOutstanding;
    private BigDecimal maintenanceTotalOverdue;
    private long maintenancePendingInvoices;
    private long maintenanceOverdueInvoices;

    // ── Sports module (tournament registrations via transactioncore) ──────────
    private BigDecimal sportsTotalCollected;
    private BigDecimal sportsEscrowHeld;
    private long sportsEscrowPendingCount;

    // ── Events module (via transactioncore) ───────────────────────────────────
    private BigDecimal eventsTotalCollected;
    private BigDecimal eventsEscrowHeld;

    // ── Marketplace module (via transactioncore) ──────────────────────────────
    private BigDecimal marketplaceTotalCollected;

    // ── Resource booking module (via transactioncore) ─────────────────────────
    private BigDecimal resourceBookingTotalCollected;

    // ── Society (community) expenses ──────────────────────────────────────────
    private BigDecimal societyApprovedExpenses;
    private long societyPendingApprovals;

    // ── Finance module invoices (global — not community-scoped) ──────────────
    private BigDecimal financeInvoicesOutstanding;
    private long financeInvoiceCount;
}
