package com.manacommunity.api.events.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "event_program")
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
    private EventCommunity event;

    @Column(name = "day_label", length = 100)
    private String dayLabel;

    @Column(name = "day_date")
    private LocalDate dayDate;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "program_type", length = 50)
    private String programType;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(length = 30)
    private String duration;

    @Column(length = 200)
    private String venue;

    @Column(length = 200)
    private String performer;

    @Column(length = 200)
    private String judge;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "requires_registration", nullable = false)
    @Builder.Default
    private boolean requiresRegistration = false;

    @Column(name = "requires_approval", nullable = false)
    @Builder.Default
    private boolean requiresApproval = false;

    @Column(name = "allow_waitlist", nullable = false)
    @Builder.Default
    private boolean allowWaitlist = true;

    @Column(name = "max_per_registration", nullable = false)
    @Builder.Default
    private int maxPerRegistration = 10;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
