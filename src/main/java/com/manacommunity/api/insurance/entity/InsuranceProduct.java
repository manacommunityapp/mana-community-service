package com.manacommunity.api.insurance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_product")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InsuranceProduct {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "partner_id", nullable = false)
    private Long partnerId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InsuranceEnums.Category category;
    @Column(length = 60)
    private String subtype;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
