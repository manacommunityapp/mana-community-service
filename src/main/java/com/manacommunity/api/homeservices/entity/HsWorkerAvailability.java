package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity @Table(name = "hs_worker_availability",
    uniqueConstraints = @UniqueConstraint(columnNames = {"worker_id", "available_date", "slot_start"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HsWorkerAvailability {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "worker_id", nullable = false) private Long workerId;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "available_date", nullable = false) private LocalDate availableDate;
    @Column(name = "slot_start", nullable = false) private LocalTime slotStart;
    @Column(name = "slot_end", nullable = false) private LocalTime slotEnd;
    @Builder.Default @Column(name = "is_booked", nullable = false) private Boolean isBooked = false;
    @Version @Builder.Default @Column(nullable = false) private Long version = 0L;
    @Column(name = "hold_token", length = 64) private String holdToken;
    @Column(name = "hold_expires_at") private java.time.Instant holdExpiresAt;
    @Builder.Default @Column(name = "travel_buffer_minutes", nullable = false) private Integer travelBufferMinutes = 15;
}
