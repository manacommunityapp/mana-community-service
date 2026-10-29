package com.manacommunity.api.academy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "academy_program")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AcademyProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String category; // SPORTS, ARTS, TECHNOLOGY, LANGUAGE, FITNESS, ACADEMIC

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN"; // OPEN, CLOSED, UPCOMING, COMPLETED

    @Column(name = "instructor_id")
    private Long instructorId;

    @Column(name = "instructor_name", length = 100)
    private String instructorName;

    @Column(name = "max_seats")
    private int maxSeats;

    @Column(name = "enrolled_count")
    @Builder.Default
    private int enrolledCount = 0;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "schedule_text", length = 200)
    private String scheduleText;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
