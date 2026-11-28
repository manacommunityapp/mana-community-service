package com.manacommunity.api.dailysubscription.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rider_batching_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RiderBatchingTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private BatchScenario scenario;

    private String riderName;
    private int totalPackages;
    private int flatsToDeliver;
    private boolean batchCheckInCompleted;
    private int individualNotificationsSent;
    private int notificationsDelivered;
    private int notificationsFailed;

    private boolean partialExitAllowed;
    private int packagesDelivered;
    private int packagesReturned;
    private boolean reconciliationCorrect;
    private String reconciliationMismatch;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum BatchScenario {
        SINGLE_RIDER_15_PACKAGES, PARTIAL_DELIVERY_EXIT,
        RIDER_TIMEOUT_NO_COLLECTION, DUPLICATE_BATCH_CHECK_IN,
        SPLIT_DELIVERY_TWO_RIDERS, WRONG_FLAT_DELIVERY_CORRECTION,
        BATCH_WITH_PERISHABLE_PRIORITY, CONCURRENT_RIDER_SAME_TOWER
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
