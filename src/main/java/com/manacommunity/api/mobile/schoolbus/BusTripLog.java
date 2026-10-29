package com.manacommunity.api.mobile.schoolbus;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "school_bus_trip_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusTripLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    private SchoolBus bus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private RegisteredChild child;

    @Column(nullable = false, length = 20)
    private String direction;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @Column(name = "departure_time", length = 10)
    private String departureTime;

    @Column(name = "arrival_time", length = 10)
    private String arrivalTime;

    @Column(name = "child_picked_up", nullable = false)
    @Builder.Default
    private boolean childPickedUp = false;

    @Column(name = "picked_up_at", length = 10)
    private String pickedUpAt;

    @Column(name = "dropped_off_at", length = 10)
    private String droppedOffAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
