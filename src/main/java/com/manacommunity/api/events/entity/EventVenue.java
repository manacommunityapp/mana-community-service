package com.manacommunity.api.events.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_venues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EventVenue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50)
    private String code;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "postal_code", length = 30)
    private String postalCode;

    private Integer capacity;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    @Column(name = "gate_info", length = 255)
    private String gateInfo;

    @Column(name = "map_coordinates", length = 100)
    private String mapCoordinates;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Community community;

    @Column(length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}