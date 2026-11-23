package com.manacommunity.api.academy.session.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "academy_session_enrollments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "student_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AcademySessionEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @CreationTimestamp
    @Column(name = "enrolled_at", updatable = false)
    private OffsetDateTime enrolledAt;

    @Column(name = "payment_status", length = 30)
    @Builder.Default
    private String paymentStatus = "FREE";

    @Column(name = "payment_ref", length = 200)
    private String paymentRef;

    private Boolean attended;

    @Column(name = "feedback_rating")
    private Short feedbackRating;

    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;
}
