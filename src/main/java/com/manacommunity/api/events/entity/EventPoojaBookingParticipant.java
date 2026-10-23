package com.manacommunity.api.events.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_pooja_booking_participants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPoojaBookingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private EventPoojaUserRegistration registration;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "gotram", length = 100)
    private String gotram;

    /** Nakshatra (birth star) — required for Sankalpam on behalf of this individual. */
    @Column(name = "nakshatra", length = 100)
    private String nakshatra;

    /** Relationship of this devotee to the booking head: head / spouse / child / parent / other. */
    @Column(name = "relation", length = 50)
    private String relation;

    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    private String qrCodeUrl;

    @Column(name = "checked_in", nullable = false)
    @Builder.Default
    private Boolean checkedIn = false;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
