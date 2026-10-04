package com.manacommunity.api.cfbos.wallet.entity;
import com.manacommunity.api.cfbos.wallet.enums.DepositStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_security_deposit")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SecurityDeposit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "deposit_type", nullable = false, length = 30)
    private String depositType;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "deposit_date", nullable = false)
    private LocalDate depositDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DepositStatus status = DepositStatus.HELD;

    @Column(name = "refund_date")
    private LocalDate refundDate;

    @Column(name = "refund_amount", precision = 18, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "deductions", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(name = "deduction_reason", columnDefinition = "TEXT")
    private String deductionReason;

    @Column(name = "receipt_id")
    private Long receiptId;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
