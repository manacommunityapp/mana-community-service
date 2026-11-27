package com.manacommunity.api.social.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "group_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_date", nullable = false)
    private OffsetDateTime eventDate;

    @Column(length = 300)
    private String location;

    @Column(name = "rsvp_deadline")
    private OffsetDateTime rsvpDeadline;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Transient
    private String rsvpStatus;

    @JsonProperty("scheduledAt")
    public OffsetDateTime getScheduledAt() {
        return eventDate;
    }
}
