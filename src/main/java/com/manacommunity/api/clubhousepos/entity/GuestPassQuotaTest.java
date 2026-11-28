package com.manacommunity.api.clubhousepos.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "guest_pass_quota_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GuestPassQuotaTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private GuestScenario scenario;

    private String amenityName;
    private String flatNumber;
    private int freeQuota;
    private int usedQuota;
    private boolean quotaExceeded;
    private double weekdayPrice;
    private double weekendPrice;
    private double chargedAmount;
    private boolean isWeekend;
    private boolean turnstileQrGenerated;
    private boolean qrSingleUseEnforced;
    private boolean qrExpired;
    private int qrExpiryMinutes;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum GuestScenario {
        FREE_QUOTA_WITHIN_LIMIT, QUOTA_EXCEEDED_WEEKDAY_CHARGE,
        QUOTA_EXCEEDED_WEEKEND_CHARGE, QR_SINGLE_USE_ENFORCED,
        QR_EXPIRED_ENTRY_BLOCKED, DOUBLE_SCAN_SAME_QR,
        GUEST_PASS_CANCELLED_REFUND, PEAK_HOUR_CAPACITY_FULL
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
