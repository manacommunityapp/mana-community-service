package com.manacommunity.api.cfbos.expense.entity;
import com.manacommunity.api.cfbos.expense.enums.ExpenseStatus;
import com.manacommunity.api.cfbos.shared.entity.CfbosBaseFields;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cfbos_expense")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CfbosExpense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_number", nullable = false, unique = true, length = 30)
    private String expenseNumber;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_category_id", nullable = false)
    private ExpenseCategory expenseCategory;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "subtotal", precision = 18, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "cgst_amount", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal cgstAmount = BigDecimal.ZERO;

    @Column(name = "sgst_amount", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    @Column(name = "igst_amount", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal igstAmount = BigDecimal.ZERO;

    @Column(name = "total_tax", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "payment_mode", length = 20)
    private String paymentMode;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "is_recurring", nullable = false)
    @Builder.Default
    private Boolean isRecurring = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ExpenseStatus status = ExpenseStatus.DRAFT;

    @Column(name = "approval_request_id")
    private Long approvalRequestId;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(name = "cost_center_id")
    private Long costCenterId;

    @Column(name = "fund_id")
    private Long fundId;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CfbosExpenseLine> lines = new ArrayList<>();

    @Embedded
    @Builder.Default
    private CfbosBaseFields baseFields = new CfbosBaseFields();

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
