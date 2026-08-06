package com.manacommunity.api.events.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "community_event_program", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private CommunityEvent event;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "day_label", length = 100)
    private String dayLabel;

    @Column(name = "day_date")
    private LocalDate dayDate;

    @Column(name = "program_type", length = 50)
    private String programType;

    @Column(name = "activity_type", length = 30)
    @Builder.Default
    private String activityType = "OPEN";

    @Column(name = "start_time", length = 20)
    private String startTime;

    @Column(length = 20)
    private String duration;

    @Column(length = 200)
    private String venue;

    @Column(length = 200)
    private String performer;

    @Column(length = 200)
    private String judge;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    private Integer capacity;

    @Column(name = "requires_registration")
    @Builder.Default
    private Boolean requiresRegistration = false;

    @Column(name = "registered_count")
    @Builder.Default
    private Integer registeredCount = 0;

    @Column(name = "waitlist_enabled")
    @Builder.Default
    private Boolean waitlistEnabled = false;

    @Column(name = "slot_status", length = 20)
    @Builder.Default
    private String slotStatus = "OPEN";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isFull() {
        return capacity != null && registeredCount != null && registeredCount >= capacity;
    }

    public int spotsLeft() {
        if (capacity == null) return Integer.MAX_VALUE;
        return Math.max(0, capacity - (registeredCount != null ? registeredCount : 0));
    }
}
