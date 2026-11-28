package com.manacommunity.api.testautomation.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ta_mobile_ui_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MobileUiTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String communityId;
    private String testSuiteCode;
    private String testSuiteName;

    @Enumerated(EnumType.STRING)
    private MobileFramework framework;

    @Enumerated(EnumType.STRING)
    private Platform platform;

    private String flowDescription;
    private String deviceModel;
    private String osVersion;
    private boolean mockLocationInjected;
    private String mockLocationCoordinates;
    private boolean mockCameraInjected;
    private String mockCameraImagePath;

    private int totalSteps;
    private int completedSteps;
    private long executionTimeMs;
    private int screenshotsCapured;
    private boolean videoRecorded;

    private boolean pushNotificationVerified;
    private boolean crossAppFlowTested;
    private String guardAppAction;
    private String residentAppAction;

    @Enumerated(EnumType.STRING)
    private ApiContractTest.TestResult testResult;

    private String failureReason;

    public enum MobileFramework { APPIUM, ESPRESSO, XCUITEST, DETOX, MAESTRO }
    public enum Platform { ANDROID, IOS, CROSS_PLATFORM }
}
