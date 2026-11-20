package com.manacommunity.api.social.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "group_event_rsvps",
       uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupEventRsvp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String response;

    @CreationTimestamp
    @Column(name = "responded_at", updatable = false)
    private OffsetDateTime respondedAt;
}
