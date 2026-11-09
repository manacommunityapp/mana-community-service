package com.manacommunity.api.socialcircle.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kitty_event", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KittyPartyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circle_id", nullable = false)
    private KittyPartyCircle circle;

    @Column(name = "event_name", nullable = false, length = 200)
    private String eventName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_type", nullable = false, length = 50)
    @Builder.Default
    private String eventType = "KITTY_MONTHLY"; // KITTY_MONTHLY, THEMED_PARTY, CASUAL_MEETUP, FESTIVAL_CELEBRATION, OTHER

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "timezone", nullable = false, length = 50)
    @Builder.Default
    private String timezone = "Asia/Kolkata";

    @Column(name = "venue_type", nullable = false, length = 30)
    @Builder.Default
    private String venueType = "HOST_HOME"; // HOST_HOME, COMMUNITY_HALL, RESTAURANT, ONLINE_VIRTUAL, OTHER

    @Column(name = "venue_address", columnDefinition = "TEXT")
    private String venueAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_user_id")
    private AppUser hostUser;

    @Column(name = "max_attendees")
    private Integer maxAttendees;

    @Column(name = "recurrence_rule", length = 30)
    @Builder.Default
    private String recurrenceRule = "NONE"; // NONE, MONTHLY, BI_WEEKLY

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "SCHEDULED"; // DRAFT, SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
