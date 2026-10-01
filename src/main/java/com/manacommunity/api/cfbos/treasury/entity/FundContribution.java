package com.manacommunity.api.cfbos.treasury.entity;

import com.manacommunity.api.cfbos.treasury.enums.ContributionStatus;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_fund_contributions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_account_id", nullable = false)
    private FundAccount fundAccount;

    private String flatNumber;
    private String tower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id")
    private AppUser resident;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    private Integer periodMonth;
    private Integer periodYear;

    private String transactionRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ContributionStatus status = ContributionStatus.CREDITED;

    private String remarks;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime creditedAt = LocalDateTime.now();
}
