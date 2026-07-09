package com.manacommunity.api.booking.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "amenity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AmenityType type = AmenityType.CLUBHOUSE;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "slot_duration_minutes")
    @Builder.Default
    private Integer slotDurationMinutes = 60;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum AmenityType { CLUBHOUSE, GYM, POOL, PARTY_HALL, TENNIS_COURT, BADMINTON_COURT, GUEST_ROOM, MEETING_ROOM }
}
