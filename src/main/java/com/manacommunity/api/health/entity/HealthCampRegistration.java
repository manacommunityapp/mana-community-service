package com.manacommunity.api.health.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "health_camp_registration", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthCampRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "camp_id", nullable = false)
    private Long campId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "token_number", nullable = false, length = 50)
    private String tokenNumber;

    @Column(name = "slot_time", length = 50)
    private String slotTime;

    @Column(name = "registered_at", nullable = false)
    @Builder.Default
    private LocalDateTime registeredAt = LocalDateTime.now();
}
