package com.manacommunity.api.wellnessbasket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Society-level delivery cluster for Mana Fresh Wellness.
 * A cluster becomes ACTIVE when {@code currentSubscriberCount}
 * reaches {@code activationThreshold} (default 15).
 */
@Getter
@Setter
@Entity
@Table(name = "wellness_community_cluster", schema = "manacommunity")
public class WellnessCommunityCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "society_name", nullable = false, length = 200)
    private String societyName;

    @Column(name = "block", length = 50)
    private String block;

    @Column(name = "city", nullable = false, length = 100)
    private String city = "Hyderabad";

    @Column(name = "lat")
    private java.math.BigDecimal lat;

    @Column(name = "lng")
    private java.math.BigDecimal lng;

    /** PENDING | ACTIVE | SUSPENDED */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "current_subscriber_count", nullable = false)
    private int currentSubscriberCount = 0;

    @Column(name = "activation_threshold", nullable = false)
    private int activationThreshold = 15;

    @Column(name = "max_capacity", nullable = false)
    private int maxCapacity = 80;

    @Column(name = "delivery_start_time", nullable = false)
    private LocalTime deliveryStartTime = LocalTime.of(6, 0);

    @Column(name = "delivery_end_time", nullable = false)
    private LocalTime deliveryEndTime = LocalTime.of(8, 0);

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
