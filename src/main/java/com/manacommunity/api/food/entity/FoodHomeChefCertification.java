package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_home_chef_certifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodHomeChefCertification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chef_id", nullable = false)
    private FoodHomeChef chef;

    @Column(name = "certification_name", nullable = false, length = 200)
    private String certificationName;

    @Column(name = "issuing_authority", length = 200)
    private String issuingAuthority;

    @Column(name = "certificate_url", length = 500)
    private String certificateUrl;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
