package com.manacommunity.api.finance.personal.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pf_transaction", schema = "manacommunity")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinanceTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private FinanceAccount account;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false, length = 20)
    private TxnType txnType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private FinanceCategory category;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @Column(length = 255)
    private String description;

    @Column(length = 255)
    private String payee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_to_account_id")
    private FinanceAccount transferToAccount;

    @Column(name = "source_module", length = 50)
    private String sourceModule;

    @Column(name = "source_ref_id")
    private Long sourceRefId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_recurring_instance")
    @Builder.Default
    private boolean recurringInstance = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TxnType {
        INCOME, EXPENSE, TRANSFER
    }
}
