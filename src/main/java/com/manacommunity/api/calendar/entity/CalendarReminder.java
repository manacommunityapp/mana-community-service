package com.manacommunity.api.calendar.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "calendar_reminder", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_source", nullable = false, length = 60)
    private String eventSource;

    @Column(name = "source_id", nullable = false, length = 100)
    private String sourceId;

    @Column(name = "remind_minutes_before", nullable = false)
    @Builder.Default
    private int remindMinutesBefore = 30;

    @Column(name = "remind_at", nullable = false)
    private Instant remindAt;

    @Column(name = "sent", nullable = false)
    @Builder.Default
    private boolean sent = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
