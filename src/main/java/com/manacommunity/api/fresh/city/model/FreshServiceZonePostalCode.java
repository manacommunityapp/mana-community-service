package com.manacommunity.api.fresh.city.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "fresh_service_zone_postal_code",
    schema = "manacommunity",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_fresh_zone_postal", columnNames = {"zone_id", "postal_code"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreshServiceZonePostalCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    @JsonIgnore
    private FreshServiceZone zone;

    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    @Column(length = 100)
    private String locality;
}
