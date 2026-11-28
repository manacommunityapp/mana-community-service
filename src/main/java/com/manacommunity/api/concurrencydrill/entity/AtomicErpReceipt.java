package com.manacommunity.api.concurrencydrill.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "atomic_erp_receipts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AtomicErpReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "flat_no", nullable = false, length = 32)
    private String flatNo;

    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @Column(name = "payment_mode", nullable = false, length = 32)
    private String paymentMode;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;
}
