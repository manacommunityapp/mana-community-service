package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_delivery_lockers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodDeliveryLocker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "location_name", nullable = false, length = 200)
    private String locationName;

    @Column(name = "locker_code", length = 50)
    private String lockerCode;

    @Column
    private Integer capacity;

    @Column
    private Integer available;

    @Column(name = "temperature_controlled", nullable = false)
    @Builder.Default
    private Boolean temperatureControlled = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
