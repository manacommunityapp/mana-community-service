package com.manacommunity.api.fresh.city.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "fresh_fulfilment_location", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreshFulfilmentLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private FreshCity city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private FreshServiceZone zone;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 50)
    @Builder.Default
    private FulfilmentLocationType locationType = FulfilmentLocationType.MANA_HUB;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }
}
