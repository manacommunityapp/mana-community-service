package com.manacommunity.api.access.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "temporary_access_token")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryAccessToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "token_code", nullable = false, unique = true, length = 64)
    private String tokenCode;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "visitor_name", length = 100)
    private String visitorName;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "max_uses", nullable = false)
    @Builder.Default
    private Integer maxUses = 1;

    @Column(name = "use_count", nullable = false)
    @Builder.Default
    private Integer useCount = 0;

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
