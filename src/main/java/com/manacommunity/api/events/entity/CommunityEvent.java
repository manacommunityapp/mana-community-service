package com.manacommunity.api.events.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 3000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private EventType type = EventType.GENERAL;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 20)
    @Builder.Default
    private LocationType locationType = LocationType.IN_PERSON;

    @Column(length = 300)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_type", length = 10)
    @Builder.Default
    private PriceType priceType = PriceType.FREE;

    private Double price;

    private Integer capacity;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "organizer_name", length = 200)
    private String organizerName;

    @Column(name = "organizer_contact", length = 200)
    private String organizerContact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EventRegistration> registrations = new ArrayList<>();

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

    public enum EventType {
        GENERAL, CONFERENCE, WORKSHOP, FUNDRAISER, PARTY, FESTIVAL, SPORTS, CULTURAL, MEETING
    }

    public enum LocationType {
        IN_PERSON, ONLINE
    }

    public enum PriceType {
        FREE, PAID
    }
}
