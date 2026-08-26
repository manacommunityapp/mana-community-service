package com.manacommunity.api.events.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "event_competitions")
public class EventCompetition extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "main_event_id")
    private Long mainEventId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "category", nullable = false, length = 150)
    private String category;

    @Column(name = "age_group", length = 100)
    private String ageGroup;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "reg_deadline")
    private LocalDate regDeadline;

    @Column(name = "fee", precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(name = "is_free")
    private Boolean isFree = true;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "venue", length = 200)
    private String venue;

    @Column(name = "rules", length = 1500)
    private String rules;

    @Column(name = "is_team_event")
    private Boolean isTeamEvent = false;

    @Column(name = "team_size")
    private Integer teamSize = 1;

    @Column(name = "needs_registration")
    private Boolean needsRegistration = true;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }

    public Long getMainEventId() { return mainEventId; }
    public void setMainEventId(Long mainEventId) { this.mainEventId = mainEventId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalDate getRegDeadline() { return regDeadline; }
    public void setRegDeadline(LocalDate regDeadline) { this.regDeadline = regDeadline; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }

    public Boolean getIsFree() { return isFree; }
    public void setIsFree(Boolean isFree) { this.isFree = isFree; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }

    public Boolean getIsTeamEvent() { return isTeamEvent; }
    public void setIsTeamEvent(Boolean isTeamEvent) { this.isTeamEvent = isTeamEvent; }

    public Integer getTeamSize() { return teamSize; }
    public void setTeamSize(Integer teamSize) { this.teamSize = teamSize; }

    public Boolean getNeedsRegistration() { return needsRegistration; }
    public void setNeedsRegistration(Boolean needsRegistration) { this.needsRegistration = needsRegistration; }
}

