package com.manacommunity.api.homeservice.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "worker_flat_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerFlatAssignmentEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "worker_id", nullable = false, length = 64)
    private String workerId;

    @Column(name = "community_id", nullable = false, length = 64)
    private String communityId;

    @Column(name = "flat_id", length = 64)
    private String flatId;

    @Column(name = "flat_number", nullable = false, length = 50)
    private String flatNumber;

    @Column(name = "tower", length = 50)
    private String tower;

    @Column(name = "resident_user_id", length = 64)
    private String residentUserId;

    @Column(name = "resident_name", length = 150)
    private String residentName;

    @Column(name = "service_category_id", length = 64)
    private String serviceCategoryId;

    @Column(name = "schedule_summary", length = 255)
    private String scheduleSummary;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
