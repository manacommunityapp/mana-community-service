package com.manacommunity.api.messaging.workload;

import com.manacommunity.api.messaging.workload.model.*;
import com.manacommunity.api.messaging.workload.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkloadMessagingRouterTest {

    private WorkloadClassifierService classifierService;
    private KafkaMessageBusAdapter kafkaAdapter;
    private RabbitMQMessageBusAdapter rabbitMQAdapter;
    private DeadLetterAndRetryEngine deadLetterEngine;
    private DualBusMessageRouter router;

    @BeforeEach
    void setUp() {
        classifierService = new WorkloadClassifierService();
        kafkaAdapter = new KafkaMessageBusAdapter();
        rabbitMQAdapter = new RabbitMQMessageBusAdapter();
        deadLetterEngine = new DeadLetterAndRetryEngine(kafkaAdapter, rabbitMQAdapter);
        router = new DualBusMessageRouter(classifierService, kafkaAdapter, rabbitMQAdapter, deadLetterEngine);
    }

    @Test
    @DisplayName("Verify Gate ANPR stream is classified as Kafka with partition ordering and 30-day retention")
    void testGateAnprClassification() {
        WorkloadProfile profile = classifierService.getProfile(WorkloadType.GATE_ANPR_STREAM);

        assertThat(profile.getRecommendedBroker()).isEqualTo(TargetBroker.KAFKA);
        assertThat(profile.isRequiresStrictOrdering()).isTrue();
        assertThat(profile.isRequiresReplay()).isTrue();
        assertThat(profile.getRetentionDays()).isEqualTo(30);
        assertThat(profile.getDefaultDestination()).isEqualTo("gate.ingress.events");

        WorkloadMessage msg = WorkloadMessage.builder()
                .workloadType(WorkloadType.GATE_ANPR_STREAM)
                .societyId("SOC-9901")
                .partitionOrRoutingKey("SOC-9901:GATE-1")
                .build();

        RoutingDecision decision = classifierService.classify(msg);
        assertThat(decision.getTargetBroker()).isEqualTo(TargetBroker.KAFKA);
        assertThat(decision.getDestination()).isEqualTo("gate.ingress.events");
        assertThat(decision.getDeadLetterDestination()).isEqualTo("gate.ingress.events.DLT");
    }

    @Test
    @DisplayName("Verify Emergency SOS is classified as RabbitMQ with max priority 10 and transient delivery")
    void testEmergencySosClassification() {
        WorkloadProfile profile = classifierService.getProfile(WorkloadType.EMERGENCY_SOS_FANOUT);

        assertThat(profile.getRecommendedBroker()).isEqualTo(TargetBroker.RABBITMQ);
        assertThat(profile.getDefaultPriority()).isEqualTo(WorkloadPriority.CRITICAL_EMERGENCY);
        assertThat(profile.getDefaultPriority().getAmqpPriorityLevel()).isEqualTo(10);
        assertThat(profile.getRetentionDays()).isEqualTo(0);
        assertThat(profile.getDefaultDestination()).isEqualTo("emergency.sos.fanout");

        WorkloadMessage msg = WorkloadMessage.builder()
                .workloadType(WorkloadType.EMERGENCY_SOS_FANOUT)
                .societyId("SOC-9901")
                .build();

        RoutingDecision decision = classifierService.classify(msg);
        assertThat(decision.getTargetBroker()).isEqualTo(TargetBroker.RABBITMQ);
        assertThat(decision.getPriority()).isEqualTo(WorkloadPriority.CRITICAL_EMERGENCY);
        assertThat(decision.getDeadLetterDestination()).isEqualTo("emergency.sos.fanout.dlx");
    }

    @Test
    @DisplayName("Verify Monthly Invoice Generation is classified as Kafka with 90-day retention for batch replay")
    void testMonthlyInvoiceBatchClassification() {
        WorkloadProfile profile = classifierService.getProfile(WorkloadType.MONTHLY_INVOICE_BATCH);

        assertThat(profile.getRecommendedBroker()).isEqualTo(TargetBroker.KAFKA);
        assertThat(profile.isRequiresStrictOrdering()).isTrue();
        assertThat(profile.isRequiresReplay()).isTrue();
        assertThat(profile.getRetentionDays()).isEqualTo(90);
        assertThat(profile.getDefaultDestination()).isEqualTo("billing.invoice.generation");
    }

    @Test
    @DisplayName("Verify VoIP Intercom Signaling is classified as RabbitMQ with high priority and sub-second delivery")
    void testVoipSignalingClassification() {
        WorkloadProfile profile = classifierService.getProfile(WorkloadType.TELEPHONY_VOIP_SIGNALING);

        assertThat(profile.getRecommendedBroker()).isEqualTo(TargetBroker.RABBITMQ);
        assertThat(profile.getDefaultPriority()).isEqualTo(WorkloadPriority.HIGH);
        assertThat(profile.getRetentionDays()).isEqualTo(0);
        assertThat(profile.getDefaultDestination()).isEqualTo("intercom.voip.signaling");
    }

    @Test
    @DisplayName("Verify Daily Help Attendance is classified as Dual-Bus (Kafka log + RabbitMQ push)")
    void testDailyHelpDualBusClassification() {
        WorkloadProfile profile = classifierService.getProfile(WorkloadType.DAILY_HELP_ATTENDANCE);

        assertThat(profile.getRecommendedBroker()).isEqualTo(TargetBroker.DUAL_BUS);
        assertThat(profile.isRequiresReplay()).isTrue();
        assertThat(profile.getRetentionDays()).isEqualTo(60);
    }

    @Test
    @DisplayName("Verify routing Gate ANPR event produces partition and offset in Kafka store")
    void testRouteGateAnpr() {
        RoutingResult result = router.routeGateAnpr("SOC-101", "GATE-NORTH", "KA-01-MJ-5544");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDispatchedBroker()).isEqualTo(TargetBroker.KAFKA);
        assertThat(result.getDeliveredDestinations()).contains("gate.ingress.events");
        assertThat(result.getPartitionAssigned()).contains("partition-").contains("@offset-");
        assertThat(kafkaAdapter.getRecordedMessageCount("gate.ingress.events")).isEqualTo(1);
    }

    @Test
    @DisplayName("Verify routing Emergency SOS delivers to RabbitMQ exchange with Priority 10")
    void testRouteEmergencySos() {
        RoutingResult result = router.routeEmergencySos("SOC-101", "Tower A - 1002", "Priya Sharma");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDispatchedBroker()).isEqualTo(TargetBroker.RABBITMQ);
        assertThat(result.getPriorityDelivered()).isEqualTo(10);
        assertThat(result.getDeliveredDestinations().get(0)).contains("emergency.sos.fanout");
        assertThat(rabbitMQAdapter.getRecordedMessageCount("emergency.sos.fanout")).isEqualTo(1);

        WorkloadMessage recorded = rabbitMQAdapter.getRecordedMessages("emergency.sos.fanout").get(0);
        assertThat(recorded.getHeaders()).containsEntry("x-priority", 10);
    }

    @Test
    @DisplayName("Verify routing Daily Help Attendance delivers concurrently to both Kafka and RabbitMQ")
    void testRouteDualBusExecution() {
        RoutingResult result = router.routeDailyHelpAttendance(
                "SOC-101",
                "STAFF-902",
                "Ramesh Kumar",
                List.of("A-101", "A-102", "B-504")
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDispatchedBroker()).isEqualTo(TargetBroker.DUAL_BUS);
        assertThat(result.getDeliveredDestinations()).hasSize(2);

        // Verify Kafka received the attendance audit log
        assertThat(kafkaAdapter.getRecordedMessageCount("staff.attendance.events")).isEqualTo(1);

        // Verify RabbitMQ received the attendance push alert
        assertThat(rabbitMQAdapter.getRecordedMessageCount("staff.attendance.events")).isEqualTo(1);
    }

    @Test
    @DisplayName("Verify Kafka poison message routes to Dead Letter Topic (.DLT)")
    void testDeadLetterToKafkaDlt() {
        WorkloadMessage msg = WorkloadMessage.builder()
                .messageId("MSG-FAIL-001")
                .workloadType(WorkloadType.MONTHLY_INVOICE_BATCH)
                .societyId("SOC-101")
                .build();

        RoutingDecision decision = classifierService.classify(msg);
        RoutingResult dltResult = deadLetterEngine.deadLetter(msg, decision, "NullPointerException in PDF Generator");

        assertThat(dltResult.isSuccess()).isTrue();
        assertThat(dltResult.getDispatchedBroker()).isEqualTo(TargetBroker.KAFKA);
        assertThat(dltResult.getDeliveredDestinations()).contains("billing.invoice.generation.DLT");
        assertThat(dltResult.getErrorMessage()).isEqualTo("NullPointerException in PDF Generator");
        assertThat(kafkaAdapter.getRecordedMessageCount("billing.invoice.generation.DLT")).isEqualTo(1);
    }

    @Test
    @DisplayName("Verify RabbitMQ failed message routes to Dead Letter Exchange (.dlx)")
    void testDeadLetterToRabbitMqDlx() {
        WorkloadMessage msg = WorkloadMessage.builder()
                .messageId("MSG-FAIL-002")
                .workloadType(WorkloadType.PAYMENT_GATEWAY_WEBHOOK)
                .societyId("SOC-101")
                .build();

        RoutingDecision decision = classifierService.classify(msg);
        RoutingResult dlxResult = deadLetterEngine.deadLetter(msg, decision, "Connection timeout to Razorpay API");

        assertThat(dlxResult.isSuccess()).isTrue();
        assertThat(dlxResult.getDispatchedBroker()).isEqualTo(TargetBroker.RABBITMQ);
        assertThat(dlxResult.getDeliveredDestinations().get(0)).contains("payment.gateway.webhooks.dlx");
        assertThat(dlxResult.getErrorMessage()).isEqualTo("Connection timeout to Razorpay API");

        List<WorkloadMessage> deadLetters = rabbitMQAdapter.getDeadLetterMessages("payment.gateway.webhooks.dlx");
        assertThat(deadLetters).hasSize(1);
        assertThat(deadLetters.get(0).getHeaders()).containsKey("x-death-reason");
    }

    @Test
    @DisplayName("Verify exponential backoff calculations adhere to jitter bounds and ceiling")
    void testExponentialBackoffWithJitter() {
        for (int retry = 0; retry < 5; retry++) {
            long backoffMs = deadLetterEngine.calculateBackoffMs(retry);
            assertThat(backoffMs).isGreaterThanOrEqualTo(100L);
            assertThat(backoffMs).isLessThanOrEqualTo(30000L);
        }

        assertThat(deadLetterEngine.shouldRetry(0)).isTrue();
        assertThat(deadLetterEngine.shouldRetry(2)).isTrue();
        assertThat(deadLetterEngine.shouldRetry(3)).isFalse();
    }
}
