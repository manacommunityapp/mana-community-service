package com.manacommunity.api.calendar.model;

import com.manacommunity.api.notification.orchestrator.NotificationEnums.NotificationPriority;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_calendar_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CalendarDomain domain;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String location;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "is_all_day")
    @Builder.Default
    private boolean isAllDay = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Column(length = 50)
    private String badge;

    @Column(name = "target_route", length = 255)
    private String targetRoute;

    @Column(name = "action_label", length = 50)
    private String actionLabel;

    @Column(name = "organizer_name", length = 100)
    private String organizerName;

    @Column(name = "is_community_wide")
    @Builder.Default
    private boolean isCommunityWide = true;

    @Column(name = "target_tower", length = 50)
    private String targetTower;

    @Column(name = "community_id")
    private Long communityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private AppUser createdByUser;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
