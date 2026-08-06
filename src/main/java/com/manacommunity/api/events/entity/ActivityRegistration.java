package com.manacommunity.api.events.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_event_activity_registration", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"program_id", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private EventProgram program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "head_count")
    @Builder.Default
    private Integer headCount = 1;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ActivityRegStatus status = ActivityRegStatus.CONFIRMED;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
    }

    public enum ActivityRegStatus { CONFIRMED, WAITLISTED, CANCELLED }
}
