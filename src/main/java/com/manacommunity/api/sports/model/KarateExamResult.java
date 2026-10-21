package com.manacommunity.api.sports.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "karate_exam_result",
        uniqueConstraints = @UniqueConstraint(name = "uq_karate_exam_result", columnNames = {"exam_id", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KarateExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private KarateExam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 20)
    private String result;               // PASS, FAIL, ABSENT

    @Column(name = "score")
    private Integer score;               // 0–100

    @Column(name = "examiner_notes", length = 500)
    private String examinerNotes;

    @Column(name = "belt_awarded", nullable = false)
    private Boolean beltAwarded = false; // true when promotion was applied

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "awarded_belt_id")
    private KarateBelt awardedBelt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        recordedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (beltAwarded == null) beltAwarded = false;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
