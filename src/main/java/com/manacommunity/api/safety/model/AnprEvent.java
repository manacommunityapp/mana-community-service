package com.manacommunity.api.safety.model;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "anpr_event", indexes = {
        @Index(name = "idx_anpr_event_community", columnList = "community_id"),
        @Index(name = "idx_anpr_event_plate", columnList = "plate_number"),
        @Index(name = "idx_anpr_event_direction", columnList = "direction"),
        @Index(name = "idx_anpr_event_gate", columnList = "gate"),
        @Index(name = "idx_anpr_event_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnprEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "plate_number", nullable = false, length = 20)
    private String plateNumber;

    @Column(name = "vehicle_type", length = 30)
    private String vehicleType;

    @Column(length = 100)
    private String gate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean recognized = false;

    @Column(name = "owner_name", length = 100)
    private String ownerName;

    @Column(name = "owner_flat", length = 20)
    private String ownerFlat;

    @Column(name = "alert_type", length = 50)
    private String alertType;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Direction {
        ENTRY, EXIT
    }
}
