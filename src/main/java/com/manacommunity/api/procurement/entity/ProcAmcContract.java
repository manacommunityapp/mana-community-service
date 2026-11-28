package com.manacommunity.api.procurement.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "proc_amc_contract")
@Getter @Setter @NoArgsConstructor
public class ProcAmcContract extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long communityId;

    @Column(nullable = false, unique = true)
    private String contractNumber;

    @Column(nullable = false)
    private String vendorName;

    @Column(nullable = false)
    private String assetCategory; // ELEVATOR, DG_SET, STP, FIRE_SYSTEM, CCTV, WATER_PUMP

    @Column(nullable = false)
    private String assetIdentifier; // e.g., "ELV-T1-01" for Elevator 1 in Tower 1

    @Column(nullable = false)
    private BigDecimal annualValue;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private LocalDate warrantyExpiry;

    @Column(nullable = false)
    private int serviceIntervalDays; // e.g., 30 for monthly, 90 for quarterly

    private int sparePartThreshold; // alert when consumed > threshold count in period

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AmcStatus status = AmcStatus.ACTIVE;

    private int renewalAlertDays = 30;
    private String contactPerson;
    private String contactPhone;
    private String slaResponseHours;
    private String terms;

    public enum AmcStatus {
        ACTIVE, EXPIRING_SOON, EXPIRED, RENEWED, CANCELLED
    }
}
