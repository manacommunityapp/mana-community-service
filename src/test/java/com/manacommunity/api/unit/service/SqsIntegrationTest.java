package com.manacommunity.api.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.manacommunity.api.grouporder.entity.GroupOrderContribution;
import com.manacommunity.api.grouporder.repository.GroupOrderContributionRepository;
import com.manacommunity.api.grouporder.sqs.GroupOrderNotificationSqsHandler;
import com.manacommunity.api.grouporder.sqs.GroupOrderNotificationSqsHandler.GroupOrderNotificationPayload;
import com.manacommunity.api.iot.mesh.sqs.IoTAlertSqsHandler;
import com.manacommunity.api.iot.mesh.sqs.IoTAlertSqsHandler.IoTAnomalyPushPayload;
import com.manacommunity.api.mobility.sqs.MobilityEventsSqsHandler;
import com.manacommunity.api.mobility.sqs.MobilityEventsSqsHandler.DriverTripOfferPayload;
import com.manacommunity.api.notification.dto.SendSmsRequest;
import com.manacommunity.api.notification.enums.MessageType;
import com.manacommunity.api.notification.service.SmsService;
import com.manacommunity.api.notification.service.SmsServiceImpl;
import com.manacommunity.api.notification.sqs.NotificationEventsSqsHandler;
import com.manacommunity.api.notification.sqs.SmsDispatchSqsHandler;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.api.sports.scheduler.SportsTournamentSchedulerService;
import com.manacommunity.api.sports.service.SportsCricHeroesService;
import com.manacommunity.api.sports.service.SportsFeedService;
import com.manacommunity.api.sports.sqs.AuctionNotificationSqsHandler;
import com.manacommunity.api.sports.sqs.AuctionNotificationSqsHandler.AuctionPushPayload;
import com.manacommunity.api.sports.sqs.ExternalSyncSqsHandler;
import com.manacommunity.api.sports.sqs.SportsEventsSqsHandler;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.platform.sqs.SqsEventEnvelope;
import com.manacommunity.platform.sqs.SqsMessageHandler;
import com.manacommunity.platform.sqs.SqsMessagePublisher;
import com.manacommunity.platform.sqs.SqsProperties;
import com.manacommunity.platform.sqs.SqsQueuePoller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AWS SQS Platform & Domain Handler Integration Tests")
class SqsIntegrationTest {

    @Mock private SqsClient sqsClient;
    @Mock private SmsServiceImpl smsServiceImpl;
    @Mock private SmsService smsService;
    @Mock private ExpoPushService expoPushService;
    @Mock private SportsFeedService sportsFeedService;
    @Mock private SportsTournamentSchedulerService schedulerService;
    @Mock private SportsCricHeroesService cricHeroesService;
    @Mock private AppUserRepository userRepository;
    @Mock private GroupOrderContributionRepository contributionRepository;

    private SqsProperties sqsProperties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        sqsProperties = new SqsProperties();
        sqsProperties.setEnabled(true);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("SqsMessagePublisher publishes envelope to SQS when enabled, and executes fallback when disabled or failing")
    void publisherSendAndFallback() throws Exception {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("https://sqs.local/mana-sms-dispatch").build());
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("msg-1").build());

        SqsMessagePublisher publisher = new SqsMessagePublisher(sqsClient, sqsProperties, objectMapper, null);
        AtomicBoolean fallbackRan = new AtomicBoolean(false);

        boolean sent = publisher.publish("mana-sms-dispatch", "SMS_DISPATCH",
                Map.of("smsMessageId", 42L), () -> fallbackRan.set(true));

        assertThat(sent).isTrue();
        assertThat(fallbackRan.get()).isFalse();

        ArgumentCaptor<SendMessageRequest> reqCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(reqCaptor.capture());
        SqsEventEnvelope decoded = objectMapper.readValue(reqCaptor.getValue().messageBody(), SqsEventEnvelope.class);
        assertThat(decoded.eventType()).isEqualTo("SMS_DISPATCH");
        assertThat(decoded.payloadJson()).contains("\"smsMessageId\":42");

        // Disabled mode triggers fallback directly
        sqsProperties.setEnabled(false);
        boolean sentDisabled = publisher.publish("mana-sms-dispatch", "SMS_DISPATCH",
                Map.of("smsMessageId", 43L), () -> fallbackRan.set(true));
        assertThat(sentDisabled).isFalse();
        assertThat(fallbackRan.get()).isTrue();
    }

    @Test
    @DisplayName("SqsQueuePoller deletes message on handler success and retains message on handler failure for DLQ retry")
    void pollerDeletesOnlyOnSuccess() throws Exception {
        SqsMessageHandler mockHandler = mock(SqsMessageHandler.class);
        when(mockHandler.queueName()).thenReturn("mana-sms-dispatch");
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("https://sqs.local/mana-sms-dispatch").build());

        String okBody = objectMapper.writeValueAsString(
                new SqsEventEnvelope("e1", "SMS_DISPATCH", "mana-sms-dispatch", Instant.now(), "{\"smsMessageId\":1}"));
        String failBody = objectMapper.writeValueAsString(
                new SqsEventEnvelope("e2", "SMS_DISPATCH", "mana-sms-dispatch", Instant.now(), "{\"smsMessageId\":2}"));

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(
                        Message.builder().messageId("m1").receiptHandle("rh-1").body(okBody).build(),
                        Message.builder().messageId("m2").receiptHandle("rh-2").body(failBody).build()
                ).build());

        doNothing().doThrow(new RuntimeException("Transient failure")).when(mockHandler).handle(any());

        SqsQueuePoller poller = new SqsQueuePoller(sqsClient, sqsProperties, objectMapper, List.of(mockHandler));
        poller.pollAllQueues();

        verify(mockHandler, times(2)).handle(any());
        verify(sqsClient, times(1)).deleteMessage(argThat((DeleteMessageRequest r) -> "rh-1".equals(r.receiptHandle())));
        verify(sqsClient, never()).deleteMessage(argThat((DeleteMessageRequest r) -> "rh-2".equals(r.receiptHandle())));
    }

    @Test
    @DisplayName("SmsDispatchSqsHandler delegates smsMessageId to SmsServiceImpl.doSend")
    void smsDispatchHandler() throws Exception {
        SmsDispatchSqsHandler handler = new SmsDispatchSqsHandler(smsServiceImpl, sqsProperties, objectMapper);
        handler.handle(new SqsEventEnvelope("e1", "SMS_DISPATCH", handler.queueName(),
                Instant.now(), "{\"smsMessageId\":77}"));

        verify(smsServiceImpl).doSend(77L);
    }

    @Test
    @DisplayName("NotificationEventsSqsHandler decodes SendSmsRequest and invokes SmsService")
    void notificationEventsHandler() throws Exception {
        NotificationEventsSqsHandler handler = new NotificationEventsSqsHandler(smsService, sqsProperties, objectMapper);
        SendSmsRequest req = SendSmsRequest.builder()
                .phoneNumber("+919876543210").templateCode("REGISTRATION_CONFIRMED")
                .messageType(MessageType.TRANSACTIONAL).userId(5L).variables(Map.of("name", "Arun")).build();

        handler.handle(new SqsEventEnvelope("e2", "REGISTRATION_CONFIRMED", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(req)));

        verify(smsService).send(argThat(r -> "REGISTRATION_CONFIRMED".equals(r.getTemplateCode()) && r.getUserId().equals(5L)));
    }

    @Test
    @DisplayName("AuctionNotificationSqsHandler sends OUTBID and PLAYER_SOLD push notifications")
    void auctionNotificationHandler() throws Exception {
        AuctionNotificationSqsHandler handler = new AuctionNotificationSqsHandler(expoPushService, sqsProperties, objectMapper);
        AuctionPushPayload payload = new AuctionPushPayload("OUTBID", 10L, "Virat", "Titans", 50000L, List.of(99L));

        handler.handle(new SqsEventEnvelope("e3", "OUTBID", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(payload)));

        verify(expoPushService).sendToUsers(eq(List.of(99L)), contains("outbid"), contains("Titans"), anyMap(), eq("events"));
    }

    @Test
    @DisplayName("IoTAlertSqsHandler sends community push alert on SENSOR_ANOMALY")
    void iotAlertHandler() throws Exception {
        IoTAlertSqsHandler handler = new IoTAlertSqsHandler(expoPushService, sqsProperties, objectMapper);
        IoTAnomalyPushPayload payload = new IoTAnomalyPushPayload(4L, 12L, "Water Tank Sensor", "HIGH", "Threshold exceeded");

        handler.handle(new SqsEventEnvelope("e4", "SENSOR_ANOMALY", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(payload)));

        verify(expoPushService).sendToCommunity(eq(4L), contains("HIGH"), eq("Threshold exceeded"), anyMap(), eq("events"));
    }

    @Test
    @DisplayName("SportsEventsSqsHandler triggers feed post creation and knockout seeding")
    void sportsEventsHandler() throws Exception {
        SportsEventsSqsHandler handler = new SportsEventsSqsHandler(sportsFeedService, schedulerService, sqsProperties, objectMapper);

        handler.handle(new SqsEventEnvelope("e5", "MATCH_RESULT_FEED_POST", handler.queueName(),
                Instant.now(), "{\"matchId\":301}"));
        verify(sportsFeedService).publishCompletedMatchPost(301L);

        handler.handle(new SqsEventEnvelope("e6", "SEED_KNOCKOUT_FROM_GROUPS", handler.queueName(),
                Instant.now(), "{\"configId\":22}"));
        verify(schedulerService).seedKnockoutFromGroups(22L);
    }

    @Test
    @DisplayName("MobilityEventsSqsHandler notifies driver user on DRIVER_TRIP_OFFER")
    void mobilityEventsHandler() throws Exception {
        MobilityEventsSqsHandler handler = new MobilityEventsSqsHandler(expoPushService, sqsProperties, objectMapper);
        DriverTripOfferPayload payload = new DriverTripOfferPayload(55L, "TRIP-55", 88L, "Main Gate");

        handler.handle(new SqsEventEnvelope("e7", "DRIVER_TRIP_OFFER", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(payload)));

        verify(expoPushService).sendToUser(eq(88L), contains("Ride"), contains("Main Gate"), anyMap(), eq("default"));
    }

    @Test
    @DisplayName("GroupOrderNotificationSqsHandler notifies distinct order contributors")
    void groupOrderNotificationHandler() throws Exception {
        GroupOrderNotificationSqsHandler handler =
                new GroupOrderNotificationSqsHandler(contributionRepository, expoPushService, sqsProperties, objectMapper);

        when(contributionRepository.findByGroupOrderId(14L)).thenReturn(List.of(
                GroupOrderContribution.builder().groupOrderId(14L).residentId(101L).build(),
                GroupOrderContribution.builder().groupOrderId(14L).residentId(102L).build(),
                GroupOrderContribution.builder().groupOrderId(14L).residentId(101L).build()
        ));

        GroupOrderNotificationPayload payload = new GroupOrderNotificationPayload(
                14L, 2L, "ORDERED", "Organic Mangoes", "Order placed with vendor");
        handler.handle(new SqsEventEnvelope("e8", "GROUP_ORDER_STATUS", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(payload)));

        verify(expoPushService).sendToUsers(eq(List.of(101L, 102L)), contains("Organic Mangoes"),
                eq("Order placed with vendor"), anyMap(), eq("events"));
    }

    @Test
    @DisplayName("ExternalSyncSqsHandler invokes CricHeroes profile refresh")
    void externalSyncHandler() throws Exception {
        AppUser requester = new AppUser();
        requester.setId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(requester));

        ExternalSyncSqsHandler handler = new ExternalSyncSqsHandler(cricHeroesService, userRepository, sqsProperties, objectMapper);
        handler.handle(new SqsEventEnvelope("e9", "CRICHEROES_REFRESH", handler.queueName(),
                Instant.now(), "{\"playerId\":70,\"userId\":5}"));

        verify(cricHeroesService).refreshProfile(70L, requester);
    }
}
