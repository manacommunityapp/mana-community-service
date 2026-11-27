package com.manacommunity.api.clubhouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pos_daily_batches", schema = "clubhouse")
@Getter
@Setter
@NoArgsConstructor
public class PosDailyBatchEntity {

    @Id
    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "terminal_id", nullable = false, length = 60)
    private String terminalId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "batch_seq", nullable = false)
    private int batchSeq;

    @Column(name = "order_count", nullable = false)
    private int orderCount;

    @Column(name = "gross_paise", nullable = false)
    private long grossPaise;

    @Column(name = "gst_paise", nullable = false)
    private long gstPaise;

    @Column(name = "flat_tab_paise", nullable = false)
    private long flatTabPaise;

    @Column(name = "wallet_paise", nullable = false)
    private long walletPaise;

    @Column(name = "offline_overrun_count", nullable = false)
    private int offlineOverrunCount;

    @Column(name = "hash_chain_root", nullable = false, length = 64)
    private String hashChainRoot;

    @Column(name = "erp_status", nullable = false, length = 40)
    private String erpStatus;

    @Column(name = "erp_voucher_ref", nullable = false, length = 80)
    private String erpVoucherRef;

    @Column(name = "closed_at", nullable = false)
    private Instant closedAt = Instant.now();
}
