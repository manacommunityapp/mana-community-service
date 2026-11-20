package com.manacommunity.api.calendar.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "personal_calendar_event", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalCalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "creator_user_id", nullable = false)
    private Long creatorUserId;

    @Column(name = "calendar_type", nullable = false, length = 40)
    private String calendarType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", length = 500)
    private String location;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "all_day", nullable = false)
    @Builder.Default
    private boolean allDay = false;

    @Column(name = "timezone", nullable = false, length = 60)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "rrule", length = 500)
    private String rrule;

    @Column(name = "rrule_until")
    private Instant rruleUntil;

    @Column(name = "is_cancelled", nullable = false)
    @Builder.Default
    private boolean cancelled = false;

    @Column(name = "color", length = 10)
    private String color;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
