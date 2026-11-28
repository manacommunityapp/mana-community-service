package com.manacommunity.api.noticeboard.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notice_delivery_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NoticeDeliveryTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private DeliveryScenario scenario;

    private String noticeType;
    private String noticeTitle;
    private int targetResidents;
    private int deliveredCount;
    private int readCount;
    private int acknowledgedCount;
    private double deliveryRate;
    private double readRate;
    private String channelUsed;
    private boolean pushSent;
    private boolean smsSent;
    private boolean emailSent;
    private boolean whatsappSent;
    private int retryAttempts;
    private boolean legalReadReceipt;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum DeliveryScenario {
        AGM_NOTICE_30_DAY_LEGAL, MAINTENANCE_HIKE_READ_RECEIPT,
        EMERGENCY_WATER_SHUTDOWN_PUSH, RULE_CHANGE_ACKNOWLEDGEMENT,
        BOARD_MEETING_MINUTES_PDF, UNDELIVERED_RESIDENT_NO_RETRY,
        MULTI_CHANNEL_DEDUP_FAILURE, SCHEDULE_PUBLISH_WRONG_TIME
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
