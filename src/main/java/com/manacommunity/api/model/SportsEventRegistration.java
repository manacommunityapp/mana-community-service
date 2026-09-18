package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity @Table(name = "sports_event_registration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsEventRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private SportsEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_member_id")
    private com.manacommunity.api.user.model.FamilyMember familyMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private AppUser reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private SportsPlayerCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", length = 20)
    private SportsEvent.MatchFormat matchType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "format_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private SportsEventFormat eventFormat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_user_id")
    private AppUser partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_family_member_id")
    private com.manacommunity.api.user.model.FamilyMember partnerFamilyMember;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @Column(name = "player_name", length = 150)
    private String playerName;

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String relation;

    @Column(name = "flat_number", length = 30)
    private String flatNumber;
    private Integer age;

    @Column(length = 30)
    private String role;

    @Column(name = "reject_reason", length = 1000)
    private String rejectReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_confirmation_status")
    private PartnerConfirmationStatus partnerConfirmationStatus;

    @Column(name = "partner_confirmed_at")
    private LocalDateTime partnerConfirmedAt;

    @Column(name = "partner_decline_reason", length = 1000)
    private String partnerDeclineReason;

    @Builder.Default
    private Boolean captainNomination = false;
    @Builder.Default
    private Boolean captainConfirmation = false;
    @Column(name = "proposed_team_name", length = 100)
    private String proposedTeamName;

    /** Admin-assigned seed for bracket draw (1 = top seed). Overrides global ranking for this event. */
    @Column(name = "seed")
    private Integer seed;

    private LocalDateTime registeredAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum RegistrationStatus { PENDING, REGISTERED, CONFIRMED, WITHDRAWN, REJECTED }
    public enum PartnerConfirmationStatus { PENDING, CONFIRMED, DECLINED }
}
