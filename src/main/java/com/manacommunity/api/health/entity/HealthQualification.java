package com.manacommunity.api.health.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_qualification", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthQualification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "degree_name", nullable = false, length = 150)
    private String degreeName;

    @Column(name = "institution_name", nullable = false, length = 255)
    private String institutionName;

    @Column(name = "year_of_completion")
    private Integer yearOfCompletion;

    @Column(length = 100)
    @Builder.Default
    private String country = "India";

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
