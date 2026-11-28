package com.manacommunity.api.gate.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Maps an RFID sticker / fob tag ID to a community resident or vehicle. */
@Entity
@Table(name = "rfid_tag_mapping", indexes = {
        @Index(name = "idx_rfid_community", columnList = "community_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_rfid_tag_community", columnNames = {"community_id", "rfid_tag_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RfidTagMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "rfid_tag_id", nullable = false, length = 100)
    private String rfidTagId;

    @Column(name = "flat_number", length = 30)
    private String flatNumber;

    @Column(name = "resident_id")
    private Long residentId;

    @Column(name = "resident_name", length = 100)
    private String residentName;

    @Column(name = "vehicle_type", length = 20)
    private String vehicleType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
