package com.manacommunity.api.cfbos.penalty.entity;
import com.manacommunity.api.cfbos.penalty.enums.PenaltyType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_penalty_config")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PenaltyConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100) private String name;
    @Builder.Default private Integer gracePeriodDays = 15;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PenaltyType lateFeeType;
    @Column(precision = 18, scale = 2) private BigDecimal lateFeeAmount;
    @Column(precision = 5, scale = 2) private BigDecimal lateFeePercentage;
    @Column(precision = 5, scale = 2) private BigDecimal interestRateAnnual;
    @Column(precision = 18, scale = 2) private BigDecimal maxPenaltyCap;
    @Builder.Default private Boolean autoApply = true;
    @Builder.Default private Boolean isActive = true;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
