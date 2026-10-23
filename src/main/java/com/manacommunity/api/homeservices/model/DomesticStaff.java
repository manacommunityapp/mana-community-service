package com.manacommunity.api.homeservices.model;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "domestic_staff", indexes = {
        @Index(name = "idx_domestic_staff_community", columnList = "community_id"),
        @Index(name = "idx_domestic_staff_role", columnList = "role"),
        @Index(name = "idx_domestic_staff_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomesticStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffRole role;

    @Column(name = "shift_time", length = 50)
    private String shiftTime;

    @Column(name = "working_towers", length = 200)
    private String workingTowers;

    @Column(name = "monthly_salary")
    private BigDecimal monthlySalary;

    @Builder.Default
    @Column(nullable = false)
    private Boolean verified = false;

    @Builder.Default
    @Column(name = "police_verified", nullable = false)
    private Boolean policeVerified = false;

    @Builder.Default
    @Column(name = "aadhaar_on_file", nullable = false)
    private Boolean aadhaarOnFile = false;

    @Builder.Default
    @Column(nullable = false)
    private Double rating = 0.0;

    @Builder.Default
    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    @Column(length = 50)
    private String experience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StaffStatus status = StaffStatus.ACTIVE;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum StaffRole {
        MAID, COOK, DRIVER, NANNY, GARDENER, WATCHMAN, HELPER
    }

    public enum StaffStatus {
        ACTIVE, ON_LEAVE, TERMINATED
    }
}
