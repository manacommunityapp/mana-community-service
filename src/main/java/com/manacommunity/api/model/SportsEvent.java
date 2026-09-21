package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "sports_event",
        indexes = @Index(name = "idx_sports_event_uuid", columnList = "uuid", unique = true))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public, non-sequential identifier used in shareable registration links
     * (e.g. /sports/register/{uuid}) so numeric DB ids are never exposed in URLs.
     * Auto-assigned on create; immutable thereafter.
     */
    @Column(name = "uuid", unique = true, updatable = false)
    private UUID uuid;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 10)
    private String icon;

    @Column(name = "min_age", nullable = false)
    private Integer minAge;

    @Column(name = "max_age", nullable = false)
    private Integer maxAge;

    @Column(name = "min_players")
    private Integer minPlayers;

    @Column(name = "max_players")
    private Integer maxPlayers;

    @Column(length = 20)
    private String gender;

    @Column(name = "players_born")
    private LocalDate playersBorn;

    @Column(nullable = false)
    private Boolean active = true;

    /**
     * When true, self-registrations land as PENDING and require an organiser to
     * confirm them before they count. When false, registrations are auto-CONFIRMED.
     * Defaults to true (apartment communities typically vet entries).
     */
    @Column(name = "admin_approval_required", nullable = false)
    @Builder.Default
    private Boolean adminApprovalRequired = true;

    /**
     * When true, mixed doubles categories strictly require complementary genders (1 Male + 1 Female).
     * When false, mixed doubles validation is relaxed/optional.
     * Defaults to true.
     */
    @Column(name = "mandatory_mixed_doubles", nullable = false)
    @Builder.Default
    private Boolean mandatoryMixedDoubles = true;

    /**
     * When true (default), younger players are permitted to opt for higher age categories (playing up).
     * When false, players are strictly bounded by both minAge and maxAge of the category.
     */
    @Column(name = "allow_higher_age_category", nullable = false)
    @Builder.Default
    private Boolean allowHigherAgeCategory = true;

    /**
     * When true (default / Option B), a player can enter multiple distinct match formats (e.g. 1 Singles + 1 Doubles).
     * When false, a player is restricted to strictly 1 event entry in this sport across the tournament.
     */
    @Column(name = "allow_multiple_categories", nullable = false)
    @Builder.Default
    private Boolean allowMultipleCategories = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id")
    private SportsMeta sport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    private LocalDate eventDateStart;
    private LocalDate eventDateEnd;

    private LocalDate registrationDateStart;
    private LocalDate registrationDateEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    private Integer maxParticipants;

    @Column(name = "start_time", length = 20)
    private String startTime;

    @Column(name = "due_time", length = 20)
    private String dueTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "auction_status", length = 20)
    private AuctionEventStatus auctionStatus;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"event"})
    @Builder.Default
    private List<SportsEventFormat> eventFormats = new ArrayList<>();

    public List<MatchFormat> getFormat() {
        if (eventFormats == null) return new ArrayList<>();
        return eventFormats.stream().map(SportsEventFormat::getFormat).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    public void setFormat(List<MatchFormat> formats) {
        if (this.eventFormats == null) {
            this.eventFormats = new ArrayList<>();
        } else {
            this.eventFormats.clear();
        }
        if (formats != null) {
            for (MatchFormat f : formats) {
                if (f != null) {
                    this.eventFormats.add(SportsEventFormat.builder().event(this).format(f).build());
                }
            }
        }
    }

    @Enumerated(EnumType.STRING)
    private TournamentType tournamentType;

    @ManyToMany
    @JoinTable(name = "sports_event_category",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<SportsPlayerCategory> categories;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"event"})
    private List<SportsNotificationScheduler> premiumNotifications;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"event", "tournament"})
    private List<SportsEventSponsor> sponsors;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @ManyToMany
    @JoinTable(name = "sports_event_dispute_committee",
            joinColumns = @JoinColumn(name = "sports_event_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<AppUser> disputeCommittee = new java.util.HashSet<>();

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "sports_event_contact",
        joinColumns = @JoinColumn(name = "sports_event_id"),
        inverseJoinColumns = @JoinColumn(name = "contact_id")
    )
    @Builder.Default
    private List<Contact> contacts = new ArrayList<>();
    
    @Column(name = "other_contacts", length = 1000)
    private String otherContacts;

    @Column(name = "auction_enabled")
    private Boolean auctionEnabled;

    @Column(columnDefinition = "TEXT")
    private String bannerImage;

    @Column(name = "tournament_level", length = 50)
    private String tournamentLevel;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private SportsEventStatus status = SportsEventStatus.DRAFT;

    // The foreign key. It MUST be nullable (nullable = true) because
    // you are creating the SportsEvent BEFORE the SportsTournament exists.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = true)
    @JsonIgnoreProperties({"sportsEvents"})
    private SportsTournament tournament;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AuctionEventStatus { DRAFT, ACTIVE, LIVE, COMPLETED, CANCELLED }
    public enum MatchFormat { SINGLES, DOUBLES, MIXED_DOUBLES, TEAM }
    public enum TournamentType { KNOCKOUT, ROUND_ROBIN, LEAGUE, KNOCKOUT_SINGLE, KNOCKOUT_DOUBLE, GROUP_PLAYOFF, CUSTOM }
}
