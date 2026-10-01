package com.manacommunity.api.cfbos.billing.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_billing_category")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BillingCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, unique = true, length = 20) private String code;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false, length = 30) private String categoryType;
    @Builder.Default private Boolean isActive = true;
    @Builder.Default private Integer displayOrder = 0;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
