package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity 
@Table(name = "sports_auction_team")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class SportsAuctionTeam {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    private SportsAuctionConfig config;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(nullable = false)
    private String teamName;

    private String ownerName;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private AppUser ownerUser;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captain_user_id")
    private AppUser captainUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private SportsEvent event;

    @Column(name = "event_id", insertable = false, updatable = false)
    private Long eventId;

    public Long getEventId() {
        return eventId != null ? eventId : (event != null ? event.getId() : null);
    }

    private String colorHex;

    @Column(nullable = false)
    private Long totalBudget;

    @Column(nullable = false)
    private Long remainingBudget;

    @Column(nullable = false)
    private Long spent;

    @Version
    private Long version;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "assignedTeam")
    private List<SportsAuctionPlayer> players;

    @Column(name = "captain_nomination")
    private Boolean captainNomination = false;

    @Column(name = "captain_confirmation")
    private Boolean captainConfirmation = false;

    private LocalDateTime createdAt;
    
    @PrePersist  void onCreate() { createdAt = LocalDateTime.now(); }
}
