package com.manacommunity.api.model;

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

@Entity
@Table(name = "sports_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
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
    private String startTime;
    private String dueTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "auction_status", length = 20)
    private AuctionEventStatus auctionStatus;

    @Column(name = "format", length = 255)
    @Convert(converter = StringListConverter.class)
    @Builder.Default
    private List<String> format = new ArrayList<>();


    @Enumerated(EnumType.STRING)
    private TournamentType tournamentType;

    @ManyToMany
    @JoinTable(name = "event_category",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<PlayerCategory> categories;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"event"})
    private List<SportsNotificationScheduler> premiumNotifications;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"event", "tournament"})
    private List<SportsEventSponsor> sponsors;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "dispute_committee_ids", length = 1000)
    private String disputeCommitteeIds;

    private String contactNumber;
    private String contactEmail;
    
    @Column(name = "other_contacts", length = 1000)
    private String otherContacts;

    private String bannerImage;
    private String tournamentLevel;

    @Column(length = 2000)
    private String description;

    // The foreign key. It MUST be nullable (nullable = true) because
    // you are creating the SportsEvent BEFORE the Tournament exists.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = true)
    @JsonIgnoreProperties({"sportsEvents"})
    private Tournament tournament;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum EventStatus { DRAFT, REGISTRATION_OPEN, REGISTRATION_CLOSED, LIVE, COMPLETED, CANCELLED }
    public enum AuctionEventStatus { DRAFT, ACTIVE, LIVE, COMPLETED, CANCELLED }
    public enum MatchFormat { SINGLES, DOUBLES, MIXED_DOUBLES, TEAM }
    public enum TournamentType { KNOCKOUT, ROUND_ROBIN, LEAGUE, KNOCKOUT_SINGLE, KNOCKOUT_DOUBLE, GROUP_PLAYOFF, CUSTOM }
}
