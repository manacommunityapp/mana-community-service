package com.manacommunity.api.health.entity;

import com.manacommunity.api.health.enums.HealthEnums.ProviderCategory;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "health_provider", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthProvider extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "provider_uuid", nullable = false, unique = true)
    @Builder.Default
    private UUID providerUuid = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private ProviderCategory category;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "brand_name", length = 255)
    private String brandName;

    @Column(name = "display_title", length = 255)
    private String displayTitle;

    @Column(name = "primary_phone", nullable = false, length = 50)
    private String primaryPhone;

    @Column(name = "emergency_phone", length = 50)
    private String emergencyPhone;

    @Column(name = "official_email", length = 255)
    private String officialEmail;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "bio_summary", columnDefinition = "TEXT")
    private String bioSummary;

    @Column(name = "logo_file_url", length = 1000)
    private String logoFileUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "trust_score", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal trustScore = new BigDecimal("5.00");

    @Column(name = "community_consultations_count", nullable = false)
    @Builder.Default
    private Integer communityConsultationsCount = 0;
}
