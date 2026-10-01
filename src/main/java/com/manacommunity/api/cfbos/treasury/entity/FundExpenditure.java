package com.manacommunity.api.cfbos.treasury.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_fund_expenditures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundExpenditure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_account_id", nullable = false)
    private FundAccount fundAccount;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    private String approvedByResolutionNo;

    private Long vendorId;
    private String vendorName;
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private AppUser approvedBy;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime disbursedAt = LocalDateTime.now();

    private String quotationAttachmentUrl;
}
