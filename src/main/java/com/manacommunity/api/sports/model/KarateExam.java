package com.manacommunity.api.sports.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "karate_exam")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KarateExam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private KarateBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_belt_id", nullable = false)
    private KarateBelt targetBelt;       // belt candidates are being tested for

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "exam_title", nullable = false, length = 150)
    private String examTitle;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, length = 20)
    private String status;               // SCHEDULED, IN_PROGRESS, COMPLETED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "SCHEDULED";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
