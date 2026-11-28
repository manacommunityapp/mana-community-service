package com.manacommunity.api.utilitymeter.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "smart_meter_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SmartMeterTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private MeterScenario scenario;

    @Enumerated(EnumType.STRING)
    private MeterType meterType;

    private String flatNumber;
    private double currentBalance;
    private double autoCutoffThreshold;
    private boolean autoCutoffTriggered;

    private int readingIntervalMinutes;
    private int outOfOrderReadings;
    private boolean readingsReordered;

    @Enumerated(EnumType.STRING)
    private TariffSource tariffSource;
    private double tariffRatePerUnit;
    private double dgTariffRatePerUnit;
    private boolean tariffSwitchCorrect;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum MeterScenario {
        OUT_OF_ORDER_READINGS, NEGATIVE_BALANCE_AUTO_CUTOFF,
        DG_TO_GRID_TARIFF_SWITCH, ZERO_READING_SPIKE_DETECTION,
        METER_OFFLINE_STALE_DATA, PREPAID_RECHARGE_BALANCE_UPDATE,
        CONCURRENT_METER_READS, TAMPER_ALERT_DETECTION
    }

    public enum MeterType { ELECTRICITY, WATER, GAS }
    public enum TariffSource { GRID, DG_GENERATOR, SOLAR }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
