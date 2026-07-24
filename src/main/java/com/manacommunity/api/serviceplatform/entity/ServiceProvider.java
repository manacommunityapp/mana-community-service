package com.manacommunity.api.serviceplatform.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Vendor;
import com.manacommunity.api.serviceplatform.entity.enums.ProviderType;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_provider", indexes = {
        @Index(name = "idx_service_provider_user", columnList = "user_id"),
        @Index(name = "idx_service_provider_community", columnList = "community_id"),
        @Index(name = "idx_service_provider_status", columnList = "verification_status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Community community;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 20)
    @Builder.Default
    private ProviderType providerType = ProviderType.INDIVIDUAL;

    @Column(name = "business_name", length = 200)
    private String businessName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "total_jobs_completed")
    @Builder.Default
    private Integer totalJobsCompleted = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "service_areas", columnDefinition = "jsonb")
    private String serviceAreas;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String certifications;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProviderServiceOffering> offerings = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
