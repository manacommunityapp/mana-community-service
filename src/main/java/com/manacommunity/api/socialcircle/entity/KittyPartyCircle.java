package com.manacommunity.api.socialcircle.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kitty_circle", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KittyPartyCircle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "circle_name", nullable = false, length = 150)
    private String circleName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "category_code", nullable = false, length = 50)
    @Builder.Default
    private String categoryCode = "KITTY_PARTY";

    @Column(name = "privacy_type", nullable = false, length = 30)
    @Builder.Default
    private String privacyType = "PRIVATE"; // COMMUNITY_PUBLIC, PRIVATE, INVITE_ONLY

    @Column(name = "approval_required", nullable = false)
    @Builder.Default
    private Boolean approvalRequired = true;

    @Column(name = "max_members")
    @Builder.Default
    private Integer maxMembers = 20;

    @Column(name = "allow_external_members", nullable = false)
    @Builder.Default
    private Boolean allowExternalMembers = true;

    @Column(name = "max_external_members")
    @Builder.Default
    private Integer maxExternalMembers = 3;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE"; // DRAFT, PENDING_APPROVAL, ACTIVE, SUSPENDED, CLOSED

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String timezone = "Asia/Kolkata";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
