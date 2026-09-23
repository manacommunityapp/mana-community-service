package com.manacommunity.api.homeservice.model.entity;

import com.manacommunity.api.homeservice.model.enums.HomeServiceReportCategory;
import com.manacommunity.api.homeservice.model.enums.HomeServiceReportSeverity;
import com.manacommunity.api.homeservice.model.enums.HomeServiceReportStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "home_service_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeServiceReportEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "community_id", nullable = false, length = 64)
    private String communityId;

    @Column(name = "worker_id", nullable = false, length = 64)
    private String workerId;

    @Column(name = "worker_name", length = 150)
    private String workerName;

    @Column(name = "reporter_user_id", nullable = false, length = 64)
    private String reporterUserId;

    @Column(name = "reporter_name", length = 150)
    private String reporterName;

    @Column(name = "flat_number", length = 50)
    private String flatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HomeServiceReportCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private HomeServiceReportSeverity severity = HomeServiceReportSeverity.MEDIUM;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private HomeServiceReportStatus status = HomeServiceReportStatus.OPEN;

    @Column(name = "admin_action_notes", columnDefinition = "TEXT")
    private String adminActionNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
