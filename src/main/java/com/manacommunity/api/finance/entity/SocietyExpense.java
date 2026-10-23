package com.manacommunity.api.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "society_expense", indexes = {
        @Index(name = "idx_society_expense_community", columnList = "community_id"),
        @Index(name = "idx_society_expense_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocietyExpense implements com.manacommunity.api.finance.common.FinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "voucher_number", length = 30)
    private String voucherNumber;

    @Column(length = 60)
    private String category;

    @Column(length = 120)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "account_id", length = 30)
    private String accountId;

    @Column(name = "account_name", length = 120)
    private String accountName;

    @Column(name = "vendor_id", length = 30)
    private String vendorId;

    @Column(name = "vendor_name", length = 120)
    private String vendorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING_CHECKER;

    @Column(name = "maker_user_id")
    private Long makerUserId;

    @Column(name = "maker_name", length = 100)
    private String makerName;

    @Column(name = "maker_date")
    private LocalDate makerDate;

    @Column(name = "checker_user_id")
    private Long checkerUserId;

    @Column(name = "checker_name", length = 100)
    private String checkerName;

    @Column(name = "checker_date")
    private LocalDate checkerDate;

    @Column(name = "checker_notes", columnDefinition = "text")
    private String checkerNotes;

    @Column(name = "approver_user_id")
    private Long approverUserId;

    @Column(name = "approver_name", length = 100)
    private String approverName;

    @Column(name = "approver_date")
    private LocalDate approverDate;

    @Column(name = "approver_notes", columnDefinition = "text")
    private String approverNotes;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(name = "payment_method", length = 40)
    private String paymentMethod;

    @Column(name = "utr_reference", length = 60)
    private String utrReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum ApprovalStatus {
        PENDING_MAKER, PENDING_CHECKER, PENDING_APPROVER, APPROVED, REJECTED, PAID
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        if (status == null) status = ApprovalStatus.PENDING_CHECKER;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    @Override
    public String getTransactionId() {
        return getVoucherNumber() != null ? getVoucherNumber() : (getId() != null ? String.valueOf(getId()) : null);
    }

    @Override
    public String getCurrency() {
        return "₹";
    }

    @Override
    public java.time.LocalDate getTransactionDate() {
        return getMakerDate() != null ? getMakerDate() : (getCreatedAt() != null ? getCreatedAt().toLocalDate() : java.time.LocalDate.now());
    }

    @Override
    public boolean isCommunityTransaction() {
        return true;
    }

    @Override
    public boolean isPersonalTransaction() {
        return false;
    }

    @Override
    public String getSourceModule() {
        return "COMMUNITY_FINANCE";
    }
}
