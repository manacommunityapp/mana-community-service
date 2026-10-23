package com.manacommunity.api.cfbos.invoice.entity;
import com.manacommunity.api.cfbos.shared.enums.CfbosStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cfbos_credit_note")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreditNote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 30) private String creditNoteNumber;
    @Column(nullable = false) private LocalDate creditNoteDate;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invoice_id", nullable = false) private CfbosInvoice invoice;
    @Column(nullable = false) private Long residentId;
    @Column(nullable = false, columnDefinition = "TEXT") private String reason;
    @Column(precision = 18, scale = 2, nullable = false) private BigDecimal subtotal;
    @Builder.Default @Column(precision = 18, scale = 2, nullable = false) private BigDecimal cgstAmount = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2, nullable = false) private BigDecimal sgstAmount = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2, nullable = false) private BigDecimal igstAmount = BigDecimal.ZERO;
    @Column(precision = 18, scale = 2, nullable = false) private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING) @Builder.Default private CfbosStatus status = CfbosStatus.DRAFT;
    private Long journalEntryId;
    @OneToMany(mappedBy = "creditNote", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<CreditNoteLine> lines = new ArrayList<>();
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
