package com.cpn.domain.events.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event extends TenantAwareEntity {

    @Column(nullable = false)
    private UUID organizerId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private LocalDateTime eventDateTime;
    private String venue;
    private String eventType; // ONLINE, OFFLINE
    private Integer rsvpCount;
    private Integer maxAttendees;
}
