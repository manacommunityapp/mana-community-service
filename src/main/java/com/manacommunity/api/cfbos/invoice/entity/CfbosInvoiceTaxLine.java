package com.manacommunity.api.cfbos.invoice.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cfbos_invoice_tax_line")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CfbosInvoiceTaxLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore
    private CfbosInvoice invoice;

    @Column(name = "tax_type", nullable = false, length = 10)
    private String taxType;

    @Column(name = "tax_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal taxRate;

    @Column(name = "taxable_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal taxableAmount;

    @Column(name = "tax_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal taxAmount;

    @Column(name = "hsn_sac_code", length = 10)
    private String hsnSacCode;
}
