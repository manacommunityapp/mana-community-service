package com.manacommunity.api.health.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "health_doctor", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthDoctor extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false, unique = true)
    private Long providerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", insertable = false, updatable = false)
    @ToString.Exclude
    private HealthProvider provider;

    @Column(name = "app_user_id")
    private Long appUserId;

    @Column(name = "primary_specialty_id", nullable = false)
    private Long primarySpecialtyId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "primary_specialty_id", insertable = false, updatable = false)
    private HealthSpecialty primarySpecialty;

    @Column(length = 20)
    private String gender;

    @Column(name = "experience_years", nullable = false)
    @Builder.Default
    private Integer experienceYears = 0;

    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal consultationFee = BigDecimal.ZERO;

    @Column(name = "follow_up_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal followUpFee = BigDecimal.ZERO;

    @Column(name = "follow_up_validity_days", nullable = false)
    @Builder.Default
    private Integer followUpValidityDays = 7;

    @Column(name = "allows_in_person", nullable = false)
    @Builder.Default
    private Boolean allowsInPerson = true;

    @Column(name = "allows_video", nullable = false)
    @Builder.Default
    private Boolean allowsVideo = true;

    @Column(name = "allows_phone", nullable = false)
    @Builder.Default
    private Boolean allowsPhone = false;

    @Column(name = "allows_home_visit", nullable = false)
    @Builder.Default
    private Boolean allowsHomeVisit = false;
}
