package com.manacommunity.api.accessibility.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "os_upgrade_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OsUpgradeTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private UpgradeScenario scenario;

    private String platform;
    private String previousVersion;
    private String upgradedVersion;
    private boolean appLaunchSuccess;
    private boolean dataIntact;
    private boolean pushTokenRefreshed;
    private boolean biometricWorking;
    private boolean deepLinksWorking;
    private boolean backgroundSyncWorking;
    private boolean permissionsRetained;
    private String deprecatedApiUsed;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum UpgradeScenario {
        ANDROID_13_TO_14_PERMISSIONS, IOS_16_TO_17_PUSH_CHANGE,
        ANDROID_12_EXACT_ALARM_REMOVED, IOS_BACKGROUND_FETCH_LIMIT,
        ANDROID_SCOPED_STORAGE_MIGRATION, WEBVIEW_CHROMIUM_UPDATE,
        BIOMETRIC_API_DEPRECATION, PLAY_STORE_TARGET_SDK_BUMP
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
