package com.manacommunity.api.cfbos.accounting.entity;

import com.manacommunity.api.cfbos.accounting.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_account")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 20)
    private String code;
    @Column(nullable = false, length = 150)
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_group_id", nullable = false)
    private AccountGroup accountGroup;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    private Account parentAccount;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;
    @Column(name = "is_system_account", nullable = false) @Builder.Default
    private Boolean isSystemAccount = false;
    @Column(name = "is_bank_account", nullable = false) @Builder.Default
    private Boolean isBankAccount = false;
    @Column(name = "is_active", nullable = false) @Builder.Default
    private Boolean isActive = true;
    @Column(name = "opening_balance", precision = 18, scale = 2, nullable = false) @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;
    @Column(name = "current_balance", precision = 18, scale = 2, nullable = false) @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;
    private String description;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
