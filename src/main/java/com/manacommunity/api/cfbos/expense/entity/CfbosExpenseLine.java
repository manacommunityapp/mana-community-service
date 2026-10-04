package com.manacommunity.api.cfbos.expense.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cfbos_expense_line")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CfbosExpenseLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    @JsonIgnore
    private CfbosExpense expense;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "rate", precision = 18, scale = 4, nullable = false)
    private BigDecimal rate;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "hsn_sac_code", length = 10)
    private String hsnSacCode;

    @Column(name = "is_taxable", nullable = false)
    @Builder.Default
    private Boolean isTaxable = false;
}
