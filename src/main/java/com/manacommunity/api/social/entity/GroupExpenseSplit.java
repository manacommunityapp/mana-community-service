package com.manacommunity.api.social.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "group_expense_splits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "amount_owed", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountOwed;

    @Column(name = "is_settled")
    @Builder.Default
    private boolean isSettled = false;

    @Column(name = "settled_at")
    private OffsetDateTime settledAt;
}
