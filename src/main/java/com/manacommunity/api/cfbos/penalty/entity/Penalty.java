package com.manacommunity.api.cfbos.penalty.entity;
import com.manacommunity.api.cfbos.penalty.enums.PenaltyStatus;
import com.manacommunity.api.cfbos.penalty.enums.PenaltyType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_penalty")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Penalty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long invoiceId;
    @Column(nullable = false) private Long residentId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "penalty_config_id", nullable = false) private PenaltyConfig penaltyConfig;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PenaltyType penaltyType;
    @Column(precision = 18, scale = 2, nullable = false) private BigDecimal amount;
    @Column(nullable = false) private LocalDate calculatedDate;
    private Long appliedToInvoiceId;
    @Enumerated(EnumType.STRING) @Builder.Default private PenaltyStatus status = PenaltyStatus.CALCULATED;
    private Long journalEntryId;
    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
