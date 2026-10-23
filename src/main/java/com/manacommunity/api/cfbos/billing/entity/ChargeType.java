package com.manacommunity.api.cfbos.billing.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_charge_type")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChargeType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_head_id", nullable = false)
    private ChargeHead chargeHead;
    @Column(nullable = false, unique = true, length = 20) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(length = 10) private String defaultHsnSacCode;
    @Builder.Default private Boolean isTaxable = true;
    @Builder.Default private Boolean isActive = true;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
