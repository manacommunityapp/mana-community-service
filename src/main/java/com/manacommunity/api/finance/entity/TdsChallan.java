package com.manacommunity.api.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tds_challan", indexes = {
        @Index(name = "idx_tds_challan_community", columnList = "community_id"),
        @Index(name = "idx_tds_challan_year_quarter", columnList = "financial_year, quarter")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TdsChallan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "financial_year", nullable = false, length = 10) // e.g. "2026-27"
    private String financialYear;

    @Column(nullable = false, length = 10) // Q1, Q2, Q3, Q4
    private String quarter;

    @Column(name = "tds_section", nullable = false, length = 20) // 194C, 194J, 194I
    private String tdsSection;

    @Column(name = "total_tds_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalTdsAmount;

    @Column(name = "challan_number", length = 40)
    private String challanNumber;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "deposited_date")
    private LocalDate depositedDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, DEPOSITED, FILED

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
