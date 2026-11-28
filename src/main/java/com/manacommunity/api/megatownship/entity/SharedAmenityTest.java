package com.manacommunity.api.megatownship.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shared_amenity_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SharedAmenityTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private AmenityScenario scenario;

    private String amenityName;
    private String bookingTower;
    private String amenityTower;
    private boolean crossTowerBookingAllowed;
    private boolean quotaEnforced;
    private int towerQuota;
    private int currentBookings;
    private boolean doubleBookingBlocked;
    private boolean billingToCorrectTower;
    private double amountCharged;
    private String billingPeriod;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum AmenityScenario {
        CROSS_TOWER_POOL_BOOKING, CLUB_HOUSE_QUOTA_EXCEEDED,
        DOUBLE_BOOKING_SAME_SLOT, BILLING_SPLIT_ACROSS_TOWERS,
        MAINTENANCE_BLOCK_ALL_TOWERS, GUEST_AMENITY_PASS_FEDERATION,
        PEAK_HOUR_SURGE_PRICING, AMENITY_SHUTDOWN_NOTIFICATION
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
