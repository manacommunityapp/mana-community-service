package com.manacommunity.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity 
@Table(name = "tournament")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class Tournament {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(length = 2000)
    private String description;

    // One Tournament contains Many Sports Events.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Community community;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"tournament"})
    private List<SportsEvent> sportsEvents = new ArrayList<>();

    private LocalDate eventDateStart;
    private LocalDate eventDateEnd;
    private LocalDate registrationDateStart;
    private LocalDate registrationDateEnd;

    private String startTime;
    private String dueTime;

    private String bannerImage;

    private String contactName;
    private String contactNumber;
    private String contactEmail;

    @Column(length = 5000)
    private String otherContacts;

    private Boolean allowAdminChat;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"tournament"})
    @Builder.Default
    private List<SportsEventSponsor> sponsors = new java.util.ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", length = 20)
    private EventStatus registrationStatus;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
