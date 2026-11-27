package com.manacommunity.api.assetaudit.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "asset_inspections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetInspection extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long assetId;
    private String qrCodeScanned;
    private String technicianId;
    private String technicianName;

    @Enumerated(EnumType.STRING)
    private InspectionType inspectionType;

    @Enumerated(EnumType.STRING)
    private InspectionResult result;

    private String readingsJson;
    private String partsReplacedJson;
    private String photoUrls;
    private String remarks;

    private double pressureReading;
    private double temperatureReading;
    private double voltageReading;
    private int runHours;

    private boolean tamperDetected;
    private boolean qrMatchVerified;
    private double gpsLat;
    private double gpsLng;

    private LocalDateTime inspectedAt;
    private long inspectionDurationSec;

    public enum InspectionType {
        ROUTINE, SAFETY_COMPLIANCE, WARRANTY_CHECK, INCIDENT_FOLLOWUP,
        PRE_MONSOON, POST_MONSOON, ANNUAL_AMC, QUARTERLY
    }

    public enum InspectionResult {
        PASS, FAIL, NEEDS_REPAIR, NEEDS_REPLACEMENT, DEFERRED, CRITICAL
    }
}
