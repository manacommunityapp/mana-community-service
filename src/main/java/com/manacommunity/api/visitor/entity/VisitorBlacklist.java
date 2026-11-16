package com.manacommunity.api.visitor.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitor_blacklist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitorBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "aadhaar_last4", length = 4)
    private String aadhaarLast4;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "blacklisted_by_user_id")
    private Long blacklistedByUserId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
