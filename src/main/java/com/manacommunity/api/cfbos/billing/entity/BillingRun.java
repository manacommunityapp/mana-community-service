package com.manacommunity.api.cfbos.billing.entity;
import com.manacommunity.api.cfbos.billing.enums.BillingRunStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cfbos_billing_run")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BillingRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 30) private String runNumber;
    @Column(nullable = false) private LocalDate billingPeriodStart;
    @Column(nullable = false) private LocalDate billingPeriodEnd;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_schedule_id")
    private BillingSchedule billingSchedule;
    @Builder.Default private String runType = "REGULAR";
    @Enumerated(EnumType.STRING)
    @Builder.Default private BillingRunStatus status = BillingRunStatus.DRAFT;
    @Builder.Default private Integer totalProperties = 0;
    @Builder.Default private BigDecimal totalAmount = BigDecimal.ZERO;
    @Builder.Default private BigDecimal totalTax = BigDecimal.ZERO;
    @Builder.Default private Boolean autoSend = false;
    private Long executedBy;
    private LocalDateTime executedAt;
    @Column(columnDefinition = "TEXT") private String errorLog;
    @OneToMany(mappedBy = "billingRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<BillingRunLine> lines = new ArrayList<>();
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
