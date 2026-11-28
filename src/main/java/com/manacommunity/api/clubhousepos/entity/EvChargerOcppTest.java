package com.manacommunity.api.clubhousepos.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ev_charger_ocpp_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EvChargerOcppTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private OcppScenario scenario;

    private String chargerId;
    private String flatNumber;
    private String vehicleNumber;
    private double kwhConsumed;
    private double ratePerKwh;
    private double totalCharge;
    private double walletBalance;
    private boolean walletDeducted;
    private boolean sessionStarted;
    private boolean sessionStopped;
    private boolean powerCutDuringCharge;
    private boolean sessionResumedAfterPowerCut;
    private int socPercent;
    private boolean overstayDetected;
    private int overstayMinutes;
    private double overstayPenalty;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum OcppScenario {
        NORMAL_CHARGE_SESSION, POWER_CUT_MID_CHARGE_RESUME,
        KWH_BILLING_ACCURACY, LOW_WALLET_CHARGE_BLOCKED,
        OVERSTAY_PENALTY_AFTER_100_PERCENT, CONCURRENT_CHARGER_BOOKING,
        OCPP_HEARTBEAT_TIMEOUT, CHARGER_FAULT_NOTIFICATION
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
