package com.manacommunity.api.events.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "event_lunch_dinners")
public class LunchDinner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "main_event_id")
    private Long mainEventId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "meal_type", nullable = false, length = 100)
    private String mealType;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "venue", length = 200)
    private String venue;

    @Column(name = "target_plates")
    private Integer targetPlates;

    @Column(name = "caterer", length = 200)
    private String caterer;

    @Column(name = "diet_type", length = 100)
    private String dietType;

    @Column(name = "fee", precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(name = "is_free")
    private Boolean isFree = true;

    @ElementCollection
    @CollectionTable(name = "event_lunch_dinner_menu_items", joinColumns = @JoinColumn(name = "lunch_dinner_id"))
    @Column(name = "menu_item")
    private List<String> menuItems = new ArrayList<>();

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LunchDinner() {
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }

    public Long getMainEventId() { return mainEventId; }
    public void setMainEventId(Long mainEventId) { this.mainEventId = mainEventId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public Integer getTargetPlates() { return targetPlates; }
    public void setTargetPlates(Integer targetPlates) { this.targetPlates = targetPlates; }

    public String getCaterer() { return caterer; }
    public void setCaterer(String caterer) { this.caterer = caterer; }

    public String getDietType() { return dietType; }
    public void setDietType(String dietType) { this.dietType = dietType; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }

    public Boolean getIsFree() { return isFree; }
    public void setIsFree(Boolean isFree) { this.isFree = isFree; }

    public List<String> getMenuItems() { return menuItems; }
    public void setMenuItems(List<String> menuItems) { this.menuItems = menuItems; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
