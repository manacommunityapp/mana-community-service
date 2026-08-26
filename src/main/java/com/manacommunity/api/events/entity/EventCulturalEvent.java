package com.manacommunity.api.events.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "event_cultural_events")
public class EventCulturalEvent extends BaseAuditEntity {

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

    @Column(name = "perf_type", length = 150)
    private String perfType;

    @Column(name = "age_group", length = 100)
    private String ageGroup;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "stage", length = 200)
    private String stage;

    @Column(name = "requirements", length = 1000)
    private String requirements;

    @Column(name = "has_backtrack")
    private Boolean hasBacktrack = true;

    @Column(name = "has_live_music")
    private Boolean hasLiveMusic = false;

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

    public String getPerfType() { return perfType; }
    public void setPerfType(String perfType) { this.perfType = perfType; }

    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public Boolean getHasBacktrack() { return hasBacktrack; }
    public void setHasBacktrack(Boolean hasBacktrack) { this.hasBacktrack = hasBacktrack; }

    public Boolean getHasLiveMusic() { return hasLiveMusic; }
    public void setHasLiveMusic(Boolean hasLiveMusic) { this.hasLiveMusic = hasLiveMusic; }

    public Boolean getNeedsRegistration() { return needsRegistration; }
    public void setNeedsRegistration(Boolean needsRegistration) { this.needsRegistration = needsRegistration; }
}

