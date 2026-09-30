package com.manacommunity.api.trip.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "community_trip", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip {
    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "community_id")
    private Long communityId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 200)
    private String destination;

    @Column(name = "departure_date", nullable = false, length = 100)
    private String departureDate;

    @Column(name = "departure_point", nullable = false, length = 200)
    private String departurePoint;

    @Column(length = 100)
    private String duration;

    @Column(name = "total_seats", nullable = false)
    @Builder.Default
    private Integer totalSeats = 20;

    @Column(name = "booked_seats", nullable = false)
    @Builder.Default
    private Integer bookedSeats = 0;

    @Column(name = "price_per_person", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal pricePerPerson = BigDecimal.ZERO;

    @Column(name = "host_name", length = 150)
    private String host;

    @Column(name = "host_flat", length = 50)
    private String hostFlat;

    @Column(length = 100)
    private String transport;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "UPCOMING";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String highlights;

    @Column(columnDefinition = "TEXT")
    private String includes;

    @Column(columnDefinition = "TEXT")
    private String excludes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}