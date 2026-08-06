package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_loyalty_gift_cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodLoyaltyGiftCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number", unique = true, length = 50)
    private String cardNumber;

    @Column(precision = 10, scale = 2)
    private BigDecimal balance;

    @Column(name = "original_amount", precision = 10, scale = 2)
    private BigDecimal originalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchased_by_id", nullable = false)
    private AppUser purchasedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gifted_to_id", nullable = true)
    private AppUser giftedTo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GiftCardStatus status;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum GiftCardStatus { ACTIVE, USED, EXPIRED }
}
