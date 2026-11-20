package com.manacommunity.api.academy.session.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "academy_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AcademySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "session_type", length = 30)
    @Builder.Default
    private String sessionType = "WORKSHOP";

    @Column(length = 20)
    @Builder.Default
    private String format = "IN_PERSON";

    @Column(length = 300)
    private String location;

    @Column(name = "online_link", length = 500)
    private String onlineLink;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(name = "max_seats")
    private Integer maxSeats;

    @Column(name = "enrolled_count")
    @Builder.Default
    private int enrolledCount = 0;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "is_recurring")
    @Builder.Default
    private boolean isRecurring = false;

    @Column(name = "recurrence_rule", length = 200)
    private String recurrenceRule;

    @Column(length = 30)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
