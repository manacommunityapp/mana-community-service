package com.manacommunity.api.events.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_event_volunteer", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventVolunteer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private CommunityEvent event;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name", length = 200)
    private String userName;

    @Column(length = 100)
    private String role;

    @Column(length = 100)
    private String zone;

    @Column(length = 100)
    private String shift;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
