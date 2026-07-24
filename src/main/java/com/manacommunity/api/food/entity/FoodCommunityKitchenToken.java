package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_community_kitchen_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodCommunityKitchenToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private FoodCommunityKitchenBooking booking;

    @Column(name = "token_number", nullable = false, length = 50)
    private String tokenNumber;

    @Column(name = "qr_code", length = 500)
    private String qrCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TokenStatus status = TokenStatus.VALID;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum TokenStatus { VALID, USED, EXPIRED }
}
