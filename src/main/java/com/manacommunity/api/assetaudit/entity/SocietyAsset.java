package com.manacommunity.api.assetaudit.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "society_assets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SocietyAsset extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long communityId;
    private String qrCode;
    private String assetTag;
    private String name;

    @Enumerated(EnumType.STRING)
    private AssetCategory category;

    private String location;
    private String towerName;
    private String floor;
    private String make;
    private String modelNumber;
    private String serialNumber;

    private LocalDate purchaseDate;
    private double purchaseCost;
    private double currentBookValue;

    @Enumerated(EnumType.STRING)
    private DepreciationMethod depreciationMethod;

    private double annualDepreciationRate;
    private int usefulLifeYears;

    private LocalDate warrantyStartDate;
    private LocalDate warrantyEndDate;
    private String warrantyProvider;
    private int warrantyClaimCount;

    @Enumerated(EnumType.STRING)
    private AssetStatus status;

    private LocalDate lastInspectionDate;
    private LocalDate nextInspectionDue;
    private int inspectionIntervalDays;
    private int totalInspections;
    private int overdueInspections;

    @Enumerated(EnumType.STRING)
    private ComplianceStatus complianceStatus;

    public enum AssetCategory {
        FIRE_EXTINGUISHER, FIRE_HYDRANT, FIRE_ALARM, STP_PUMP, WATER_PUMP,
        DIESEL_GENERATOR, ELEVATOR, GYM_EQUIPMENT, CLUBHOUSE_FURNITURE,
        CCTV_CAMERA, INTERCOM_PANEL, TRANSFORMER, SOLAR_PANEL, WATER_TANK
    }

    public enum DepreciationMethod {
        STRAIGHT_LINE, WRITTEN_DOWN_VALUE, UNITS_OF_PRODUCTION
    }

    public enum AssetStatus {
        ACTIVE, UNDER_MAINTENANCE, DECOMMISSIONED, DISPOSED, WARRANTY_CLAIM
    }

    public enum ComplianceStatus {
        COMPLIANT, DUE_SOON, OVERDUE, NON_COMPLIANT, EXEMPT
    }
}
