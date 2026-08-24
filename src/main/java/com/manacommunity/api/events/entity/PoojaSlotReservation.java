package com.manacommunity.api.events.entity;

import com.manacommunity.api.events.enums.ReservationStatus;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "event_pooja_slot_reservation",
    indexes = {
        @Index(name = "idx_psr_schedule_status", columnList = "schedule_id, status, expires_at"),
        @Index(name = "idx_psr_user",            columnList = "user_id"),
        @Index(name = "idx_psr_expires",         columnList = "expires_at, status")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoojaSlotReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private PoojaSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** Set once the registration is created and this reservation is confirmed. */
    @Column(name = "registration_id")
    private Long registrationId;

    @Column(name = "reserved_family_count", nullable = false)
    @Builder.Default
    private Integer reservedFamilyCount = 1;

    @Column(name = "reserved_devotee_count", nullable = false)
    @Builder.Default
    private Integer reservedDevoteeCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.RESERVED;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Caller-supplied UUID to prevent double-reservations on retry. */
    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    /** Sankalpam token number assigned at reservation time. Returned on idempotency hits. */
    @Column(name = "token_number")
    private Integer tokenNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
