package com.manacommunity.api.socialcircle.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kitty_circle_member", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KittyPartyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circle_id", nullable = false)
    private KittyPartyCircle circle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "role_code", nullable = false, length = 30)
    @Builder.Default
    private String roleCode = "MEMBER"; // OWNER, ADMIN, TREASURER, HOST, MEMBER

    @Column(name = "membership_status", nullable = false, length = 30)
    @Builder.Default
    private String membershipStatus = "ACTIVE"; // REQUESTED, INVITED, ACTIVE, REJECTED, SUSPENDED, LEFT, REMOVED

    @Column(name = "is_external", nullable = false)
    @Builder.Default
    private Boolean isExternal = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_user_id")
    private AppUser sponsorUser;

    @Column(name = "external_society_name", length = 150)
    private String externalSocietyName;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private AppUser approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (joinedAt == null && "ACTIVE".equals(membershipStatus)) {
            joinedAt = createdAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
