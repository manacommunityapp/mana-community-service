package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_recognitions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrRecognition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "giver_employee_id", nullable = false)
    private HrEmployee giverEmployee;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "receiver_employee_id", nullable = false)
    private HrEmployee receiverEmployee;
    @Column(name = "recognition_type", nullable = false, length = 30) @Builder.Default private String recognitionType = "KUDOS";
    @Column(name = "message", nullable = false, columnDefinition = "TEXT") private String message;
    @Column(name = "points") @Builder.Default private Integer points = 0;
    @Column(name = "is_public", nullable = false) @Builder.Default private Boolean isPublic = true;
    @Column(name = "category", length = 100) private String category;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
