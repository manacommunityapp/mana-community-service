package com.manacommunity.api.cfbos.invoice.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cfbos_credit_note_line")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreditNoteLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "credit_note_id", nullable = false) @JsonIgnore private CreditNote creditNote;
    private Long invoiceLineId;
    @Column(nullable = false, length = 255) private String description;
    @Builder.Default private BigDecimal quantity = BigDecimal.ONE;
    private BigDecimal rate;
    private BigDecimal amount;
    @Builder.Default private BigDecimal cgstAmount = BigDecimal.ZERO;
    @Builder.Default private BigDecimal sgstAmount = BigDecimal.ZERO;
    @Builder.Default private BigDecimal igstAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount;
}
