package com.manacommunity.api.accessibility.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "device_fragmentation_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceFragmentationTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private DeviceScenario scenario;

    private String deviceModel;
    private String osName;
    private String osVersion;
    private String screenSize;
    private int ramMb;
    private String browserEngine;
    private boolean responsiveLayout;
    private boolean touchTargetSize;
    private boolean notchSafeArea;
    private boolean lowEndPerformance;
    private double loadTimeSec;
    private double targetLoadTimeSec;
    private boolean crashFree;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum DeviceScenario {
        BUDGET_ANDROID_2GB_RAM, IPHONE_SE_SMALL_SCREEN,
        ANDROID_14_PERMISSION_CHANGE, IOS_17_PUSH_UPDATE,
        JIOBOOK_LOW_END_BROWSER, TABLET_LANDSCAPE_LAYOUT,
        FOLDABLE_SCREEN_TRANSITION, WEBVIEW_OLDER_CHROME
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
