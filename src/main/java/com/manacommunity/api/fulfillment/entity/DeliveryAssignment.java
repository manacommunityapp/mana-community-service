package com.manacommunity.api.fulfillment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "delivery_assignments")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DeliveryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "delivery_person_id")
    private Long deliveryPersonId;

    @Column(name = "delivery_person_name", length = 200)
    private String deliveryPersonName;

    @Column(name = "delivery_person_phone", length = 20)
    private String deliveryPersonPhone;

    @Column(name = "estimated_arrival")
    private Instant estimatedArrival;

    @Column(name = "actual_delivery")
    private Instant actualDelivery;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = Instant.now();
        if (status == null) status = "ASSIGNED";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
