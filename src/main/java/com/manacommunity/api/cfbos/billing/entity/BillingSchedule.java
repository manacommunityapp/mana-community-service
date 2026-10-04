package com.manacommunity.api.cfbos.billing.entity;
import com.manacommunity.api.cfbos.billing.enums.BillingScheduleFrequency;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_billing_schedule")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BillingSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100) private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private BillingScheduleFrequency frequency;
    @Builder.Default private Integer billingDay = 1;
    @Builder.Default private Integer dueDayOffset = 15;
    @Builder.Default private Integer advanceBillingDays = 0;
    @Builder.Default private Boolean isActive = true;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
