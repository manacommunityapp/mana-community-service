package com.manacommunity.api.datamigration.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dm_app_upgrade_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUpgradeTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private UpgradeScenario scenario;

    private String fromVersion;
    private String toVersion;
    private String deviceModel;
    private String platform;

    private int sqliteRecordsBefore;
    private int sqliteRecordsAfter;
    private boolean dataLoss;
    private int recordsLost;
    private String dataLossDetail;

    private boolean offlineQueuePreserved;
    private int queuedItemsBefore;
    private int queuedItemsAfter;
    private int queuedItemsSyncedPostUpgrade;

    private boolean schemaMigrationSuccessful;
    private String schemaMigrationError;
    private long upgradeDurationMs;

    @Enumerated(EnumType.STRING)
    private BulkOnboardingTest.TestResult testResult;

    private String failureReason;

    public enum UpgradeScenario {
        CLEAN_UPGRADE, UPGRADE_WITH_PENDING_OFFLINE_QUEUE,
        UPGRADE_WITH_SCHEMA_CHANGE, DOWNGRADE_ATTEMPT,
        UPGRADE_LOW_STORAGE, UPGRADE_DURING_SYNC
    }
}
