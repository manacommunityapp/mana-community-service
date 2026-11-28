package com.manacommunity.api.noticeboard.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "digital_signage_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DigitalSignageTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private SignageScenario scenario;

    private String screenLocation;
    private String contentType;
    private int scheduledSlots;
    private int displayedSlots;
    private boolean autoRotation;
    private int rotationIntervalSec;
    private boolean emergencyOverride;
    private boolean contentExpired;
    private boolean offlineMode;
    private String resolution;
    private boolean brightnessAutoAdjust;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum SignageScenario {
        LOBBY_SCREEN_AUTO_ROTATE, EMERGENCY_OVERRIDE_ALL_SCREENS,
        EXPIRED_CONTENT_NOT_REMOVED, OFFLINE_CACHED_CONTENT,
        PORTRAIT_LANDSCAPE_SWITCH, BRIGHTNESS_DAYLIGHT_ADJUST,
        MULTI_TOWER_CONTENT_SYNC, VENDOR_AD_SLOT_ROTATION
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
