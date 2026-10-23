package com.manacommunity.api.homeservice.model.entity;

import com.manacommunity.api.homeservice.model.enums.HomeServiceRequirementStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "home_service_requirements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeServiceRequirementEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "community_id", nullable = false, length = 64)
    private String communityId;

    @Column(name = "resident_user_id", nullable = false, length = 64)
    private String residentUserId;

    @Column(name = "resident_name", length = 150)
    private String residentName;

    @Column(name = "flat_number", nullable = false, length = 50)
    private String flatNumber;

    @Column(name = "tower", length = 50)
    private String tower;

    @Column(name = "category_id", nullable = false, length = 64)
    private String categoryId;

    @Column(name = "category_name", length = 100)
    private String categoryName;

    @Column(name = "frequency", length = 30)
    private String frequency;

    @Column(name = "preferred_days", length = 200)
    private String preferredDays;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "preferred_start_time")
    private LocalTime preferredStartTime;

    @Column(name = "preferred_end_time")
    private LocalTime preferredEndTime;

    @Column(name = "budget_min", precision = 10, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 10, scale = 2)
    private BigDecimal budgetMax;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private HomeServiceRequirementStatus status = HomeServiceRequirementStatus.OPEN;

    @Column(name = "responses_count")
    @Builder.Default
    private Integer responsesCount = 0;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
