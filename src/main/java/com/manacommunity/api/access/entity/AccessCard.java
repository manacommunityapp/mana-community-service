package com.manacommunity.api.access.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_card")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "flat_no", length = 30)
    private String flatNo;

    @Column(name = "card_number", nullable = false, length = 60)
    private String cardNumber;

    @Column(name = "card_type", length = 20, nullable = false)
    @Builder.Default
    private String cardType = "RFID";

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "deactivated_reason", columnDefinition = "TEXT")
    private String deactivatedReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
