package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_training_programs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrTrainingProgram {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @Column(name = "title", nullable = false, length = 300) private String title;
    @Column(name = "description", columnDefinition = "TEXT") private String description;
    @Column(name = "training_type", length = 50) private String trainingType;
    @Column(name = "provider", length = 200) private String provider;
    @Column(name = "duration_hours") private Integer durationHours;
    @Column(name = "is_mandatory", nullable = false) @Builder.Default private Boolean isMandatory = false;
    @Column(name = "max_participants") private Integer maxParticipants;
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "ACTIVE";
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
