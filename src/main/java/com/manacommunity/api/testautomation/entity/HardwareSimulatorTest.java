package com.manacommunity.api.testautomation.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ta_hardware_simulator_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HardwareSimulatorTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String communityId;
    private String testSuiteCode;
    private String testSuiteName;

    @Enumerated(EnumType.STRING)
    private SimulatorType simulatorType;

    private String simulatorLanguage;
    private int concurrentDevices;
    private int payloadsPerSecond;
    private long testDurationSeconds;
    private long totalPayloadsSent;
    private long totalPayloadsProcessed;
    private long droppedPayloads;
    private double dropRatePercent;

    private int p50LatencyMs;
    private int p95LatencyMs;
    private int p99LatencyMs;
    private int maxLatencyMs;

    private boolean stagingEnvironment;
    private String protocolUsed;
    private long peakMemoryMb;
    private double peakCpuPercent;

    @Enumerated(EnumType.STRING)
    private ApiContractTest.TestResult testResult;

    private String failureReason;

    public enum SimulatorType {
        RFID_READER, OCPP_EV_CHARGER, SMART_WATER_METER,
        BOOM_BARRIER, INTERCOM_SIP, CCTV_STREAM, SMART_LOCK_BLE
    }
}
