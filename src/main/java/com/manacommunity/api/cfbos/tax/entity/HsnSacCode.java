package com.manacommunity.api.cfbos.tax.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_hsn_sac_code")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsnSacCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false)
    private String description;

    @Column(name = "code_type", nullable = false, length = 3)
    private String codeType;

    @Column(name = "default_gst_rate", precision = 5, scale = 2)
    private BigDecimal defaultGstRate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
