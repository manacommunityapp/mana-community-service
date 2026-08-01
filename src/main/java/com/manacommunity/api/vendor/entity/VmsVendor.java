package com.manacommunity.api.vendor.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vms_vendors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsVendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(length = 200)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 40)
    @Builder.Default
    private BusinessType businessType = BusinessType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private VendorStatus status = VendorStatus.PENDING;

    @Column(length = 2000)
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "gst_number", length = 30)
    private String gstNumber;

    @Column(length = 20)
    private String pan;

    @Column(name = "trade_license", length = 100)
    private String tradeLicense;

    @Column(name = "insurance_number", length = 100)
    private String insuranceNumber;

    @Column(length = 300)
    private String website;

    @Column(name = "years_of_experience")
    @Builder.Default
    private Integer yearsOfExperience = 0;

    @Column(name = "number_of_employees")
    @Builder.Default
    private Integer numberOfEmployees = 1;

    @Column(name = "emergency_contact", length = 20)
    private String emergencyContact;

    @Column(name = "emergency_name", length = 120)
    private String emergencyName;

    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "total_ratings")
    @Builder.Default
    private Integer totalRatings = 0;

    @Column(name = "total_jobs_completed")
    @Builder.Default
    private Integer totalJobsCompleted = 0;

    @Column(name = "commission_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @Column(name = "vacation_mode", nullable = false)
    @Builder.Default
    private Boolean vacationMode = false;

    @Column(name = "emergency_available", nullable = false)
    @Builder.Default
    private Boolean emergencyAvailable = false;

    @Column(name = "max_daily_jobs")
    @Builder.Default
    private Integer maxDailyJobs = 10;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private VmsVendorCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private AppUser approvedByUser;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = VendorStatus.PENDING;
        if (businessType == null) businessType = BusinessType.INDIVIDUAL;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum VendorStatus { PENDING, APPROVED, SUSPENDED, REJECTED, BLOCKED }
    public enum BusinessType { INDIVIDUAL, COMPANY, PARTNERSHIP, PROPRIETORSHIP }
}
