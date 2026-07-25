package com.manacommunity.api.cfbos.accounting.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_accounting_period")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountingPeriod {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;
    @Column(nullable = false, length = 50)
    private String name;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;
    @Column(length = 20, nullable = false) @Builder.Default
    private String status = "OPEN";
    private Long closedBy;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
