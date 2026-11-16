package com.manacommunity.api.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bbps_bill_fetch")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BbpsBillFetch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "biller_id", nullable = false, length = 64)
    private String billerId;

    @Column(name = "biller_name", length = 200)
    private String billerName;

    @Column(name = "consumer_id", nullable = false, length = 100)
    private String consumerId;

    @Column(name = "bill_number", length = 100)
    private String billNumber;

    @Column(name = "bill_amount", precision = 14, scale = 2)
    private BigDecimal billAmount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private String status = "FETCHED";

    @Column(name = "payment_order_id")
    private Long paymentOrderId;

    @Column(name = "fetched_at", nullable = false)
    @Builder.Default
    private LocalDateTime fetchedAt = LocalDateTime.now();

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
