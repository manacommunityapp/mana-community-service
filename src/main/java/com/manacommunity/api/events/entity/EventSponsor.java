package com.manacommunity.api.events.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_sponsor")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSponsor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CommunityEvent event;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private SponsorTier tier = SponsorTier.SILVER;

    @Column(name = "amount_pledged")
    private Double amountPledged;

    @Column(name = "amount_received")
    private Double amountReceived;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "contact_name", length = 200)
    private String contactName;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "contact_email", length = 200)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private SponsorStatus status = SponsorStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SponsorTier {
        PLATINUM, GOLD, SILVER, BRONZE
    }

    public enum SponsorStatus {
        PENDING, CONFIRMED, DECLINED
    }
}
