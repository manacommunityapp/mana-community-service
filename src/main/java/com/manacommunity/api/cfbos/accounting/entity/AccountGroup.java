package com.manacommunity.api.cfbos.accounting.entity;

import com.manacommunity.api.cfbos.accounting.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_account_group")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 10)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_group_id")
    private AccountGroup parentGroup;
    @Column(name = "display_order", nullable = false) @Builder.Default
    private Integer displayOrder = 0;
    @Column(name = "is_system", nullable = false) @Builder.Default
    private Boolean isSystem = false;
    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
