package com.manacommunity.api.finance.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "finance_document_lines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceDocumentLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private FinanceDocument document;

    @Column(length = 200)
    private String item;

    @Column(length = 400)
    private String description;

    @Column(nullable = false)
    private Integer qty;

    @Column(precision = 14, scale = 2) private BigDecimal cost;
    /** Discount percent. */
    @Column(precision = 6, scale = 2)  private BigDecimal disc;
    /** Tax percent. */
    @Column(precision = 6, scale = 2)  private BigDecimal tax;
    @Column(name = "line_total", precision = 14, scale = 2) private BigDecimal lineTotal;
}
