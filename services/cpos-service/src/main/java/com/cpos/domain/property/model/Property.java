package com.cpos.domain.property.model;

import com.cpos.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Property — The Primary Business Entity of CPOS.
 * Every feature, document, owner, tenant, finance record, and AI insight
 * is anchored to a Property. This is the Digital Twin of a real-world property.
 */
@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property extends TenantAwareEntity {

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "tower_id")
    private UUID towerId;

    @Column(name = "floor_id")
    private UUID floorId;

    // Digital Identity
    @Column(name = "property_code", unique = true, nullable = false)
    private String propertyCode; // CPOS-{communityCode}-{unitNumber}

    @Column(name = "property_name")
    private String propertyName;

    @Column(name = "property_type_code", nullable = false)
    private String propertyTypeCode;

    // Location
    @Column(name = "unit_number", nullable = false)
    private String unitNumber;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "wing")
    private String wing;

    @Column(name = "facing")
    private String facing;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    // Area
    @Column(name = "carpet_area", precision = 10, scale = 2)
    private BigDecimal carpetArea;

    @Column(name = "built_up_area", precision = 10, scale = 2)
    private BigDecimal builtUpArea;

    @Column(name = "super_built_up_area", precision = 10, scale = 2)
    private BigDecimal superBuiltUpArea;

    // Configuration
    @Column(name = "bedrooms")
    private Integer bedrooms;

    @Column(name = "bathrooms")
    private Integer bathrooms;

    @Column(name = "balconies")
    private Integer balconies;

    @Column(name = "furnished_status")
    private String furnishedStatus; // UNFURNISHED, SEMI_FURNISHED, FULLY_FURNISHED

    // Parking
    @Column(name = "covered_parking")
    private Integer coveredParking;

    @Column(name = "open_parking")
    private Integer openParking;

    // Status
    @Column(name = "construction_status")
    private String constructionStatus; // UNDER_CONSTRUCTION, READY, POSSESSION_GIVEN

    @Column(name = "occupancy_status")
    private String occupancyStatus; // OWNER_OCCUPIED, TENANT_OCCUPIED, VACANT, RESERVED

    @Column(name = "property_status")
    private String propertyStatus; // ACTIVE, LISTED_FOR_SALE, LISTED_FOR_RENT, UNDER_RENOVATION

    // Registration
    @Column(name = "khata_number")
    private String khataNumber;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "rera_unit_number")
    private String reraUnitNumber;

    // Construction
    @Column(name = "construction_year")
    private Integer constructionYear;

    @Column(name = "possession_date")
    private LocalDate possessionDate;

    // Media
    @Column(name = "primary_image_url")
    private String primaryImageUrl;

    // Financial
    @Column(name = "last_purchase_price", precision = 15, scale = 2)
    private BigDecimal lastPurchasePrice;

    @Column(name = "current_market_value", precision = 15, scale = 2)
    private BigDecimal currentMarketValue;

    @Column(name = "monthly_maintenance", precision = 10, scale = 2)
    private BigDecimal monthlyMaintenance;
}
