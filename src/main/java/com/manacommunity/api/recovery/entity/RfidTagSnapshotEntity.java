package com.manacommunity.api.recovery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "rfid_tag_snapshots", schema = "recovery")
@Getter
@Setter
public class RfidTagSnapshotEntity {

    @Id
    @Column(name = "tag_id", nullable = false, length = 64)
    private String tagId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "vehicle_number", nullable = false, length = 32)
    private String vehicleNumber;

    @Column(name = "flat_unit_code", nullable = false, length = 40)
    private String flatUnitCode;

    @Column(name = "tag_status", nullable = false, length = 24)
    private String tagStatus;

    @Column(name = "pre_incident_status", nullable = false, length = 24)
    private String preIncidentStatus;

    @Column(name = "blacklisted", nullable = false)
    private Boolean blacklisted = Boolean.FALSE;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
