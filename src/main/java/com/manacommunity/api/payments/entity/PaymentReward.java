package com.manacommunity.api.payments.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "payment_reward")
@Data
public class PaymentReward {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "source_module", nullable = false, length = 100) private String sourceModule;
    @Column(name = "reference_id") private Long referenceId;
    @Column(name = "reward_type", nullable = false, length = 100) private String rewardType;
    @Column private Integer points = 0;
    @Column(length = 500) private String description;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column private Boolean redeemed = false;
    @Column(name = "redeemed_at") private Instant redeemedAt;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
