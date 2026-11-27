package com.manacommunity.api.parking.entity;

import com.manacommunity.api.model.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "vehicle_stickers")
@Data
@EqualsAndHashCode(callSuper = true)
public class VehicleSticker extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "vehicle_number", nullable = false)
    private String vehicleNumber;

    @Column(name = "sticker_code", nullable = false, unique = true)
    private String stickerCode;

    @Column(name = "flat_number", nullable = false)
    private String flatNumber;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "vehicle_type", nullable = false)
    private String vehicleType; // SEDAN, SUV, BIKE, EV

    @Column(name = "assigned_bay_id")
    private Long assignedBayId;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "owner_phone_hash")
    private String ownerPhoneHash; // hashed for privacy-preserving lookups
}
