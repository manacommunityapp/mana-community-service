package com.manacommunity.api.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.email.sqs.EmailDispatchSqsHandler;
import com.manacommunity.api.email.sqs.EmailDispatchSqsHandler.EmailDispatchPayload;
import com.manacommunity.api.grouporder.entity.GroupOrderContribution;
import com.manacommunity.api.grouporder.repository.GroupOrderContributionRepository;
import com.manacommunity.api.grouporder.sqs.GroupOrderNotificationSqsHandler;
import com.manacommunity.api.grouporder.sqs.GroupOrderNotificationSqsHandler.GroupOrderNotificationPayload;
import com.manacommunity.api.groupbuying.service.GroupBuyPaymentConfirmationService;
import com.manacommunity.api.healthguard.sqs.EmergencySosSqsHandler;
import com.manacommunity.api.healthguard.sqs.EmergencySosSqsHandler.SosEscalatedPayload;
import com.manacommunity.api.healthguard.sqs.EmergencySosSqsHandler.SosTriggeredPayload;
import com.manacommunity.api.iot.mesh.sqs.IoTAlertSqsHandler;
import com.manacommunity.api.iot.mesh.sqs.IoTAlertSqsHandler.IoTAnomalyPushPayload;
import com.manacommunity.api.mobility.sqs.MobilityEventsSqsHandler;
import com.manacommunity.api.mobility.sqs.MobilityEventsSqsHandler.DriverTripOfferPayload;
import com.manacommunity.api.mobility.sqs.MobilityEventsSqsHandler.GroupCancelledPayload;
import com.manacommunity.api.mobility.sqs.MobilityEventsSqsHandler.GroupJoinPayload;
import com.manacommunity.api.mobility.sqs.MobilityEventsSqsHandler.GroupLeavePayload;
import com.manacommunity.api.mobility.sqs.MobilityEventsSqsHandler.WaitlistPromotedPayload;
import com.manacommunity.api.mobility.sqs.MobilityEventsSqsHandler.WaitlistedPayload;
import com.manacommunity.api.events.service.EventReportService;
import com.manacommunity.api.events.sqs.ReportJobSqsHandler;
import com.manacommunity.api.events.sqs.ReportJobSqsHandler.BatchInvoicePayload;
import com.manacommunity.api.notification.dto.SendSmsRequest;
import com.manacommunity.api.notification.enums.MessageType;
import com.manacommunity.api.notification.orchestrator.UnifiedNotificationService;
import com.manacommunity.api.notification.orchestrator.provider.WhatsAppProvider;
import com.manacommunity.api.notification.service.SmsService;
import com.manacommunity.api.notification.service.SmsServiceImpl;
import com.manacommunity.api.notification.sqs.NotificationEventsSqsHandler;
import com.manacommunity.api.notification.sqs.NotificationEventsSqsHandler.UnifiedPushPayload;
import com.manacommunity.api.notification.sqs.NotificationEventsSqsHandler.UnifiedWhatsAppPayload;
import com.manacommunity.api.notification.sqs.SmsDispatchSqsHandler;
import com.manacommunity.api.payments.sqs.PaymentEventsSqsHandler;
import com.manacommunity.api.payments.sqs.PaymentEventsSqsHandler.CashfreeBusinessConfirmationPayload;
import com.manacommunity.api.payments.sqs.PaymentEventsSqsHandler.PaymentNotificationPayload;
import com.manacommunity.api.service.BillingService;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.api.sports.model.SportsEventRegistration;
import com.manacommunity.api.sports.repository.SportsEventRegistrationRepository;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.math.BigDecimal;
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
    @Mock private EmailService emailService;
    @Mock private ExpoPushService expoPushService;
    @Mock private SportsFeedService sportsFeedService;
    @Mock private SportsTournamentSchedulerService schedulerService;
    @Mock private SportsCricHeroesService cricHeroesService;
    @Mock private AppUserRepository userRepository;
    @Mock private GroupOrderContributionRepository contributionRepository;
    @Mock private ObjectProvider<UnifiedNotificationService> notificationServiceProvider;
    @Mock private UnifiedNotificationService unifiedNotificationService;
    @Mock private SportsEventRegistrationRepository sportsRegistrationRepository;
    @Mock private ObjectProvider<GroupBuyPaymentConfirmationService> groupBuyConfirmationProvider;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private WhatsAppProvider whatsAppProvider;
    @Mock private EventReportService eventReportService;
    @Mock private ObjectProvider<BillingService> billingServiceProvider;
    @Mock private BillingService billingService;

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

        verify(expoPushService).sendToUsers(eq(List.of(99L)), contains("outbid"), contains("Titans"), anyMap(), eq("auctions"));
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
    @DisplayName("MobilityEventsSqsHandler routes carpool join, waitlist, leave, and cancel events")
    void mobilityCarpoolEventsHandler() throws Exception {
        MobilityEventsSqsHandler handler = new MobilityEventsSqsHandler(expoPushService, sqsProperties, objectMapper);

        handler.handle(new SqsEventEnvelope("m1", "MOBILITY_GROUP_JOIN", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(new GroupJoinPayload(10L, 20L, "Riya", "airport"))));
        verify(expoPushService).sendToUser(eq(20L), eq("New passenger joined"), contains("Riya"), anyMap(), eq("default"));

        handler.handle(new SqsEventEnvelope("m2", "MOBILITY_WAITLIST_PROMOTED", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(new WaitlistPromotedPayload(10L, 30L, "Gate 1", "airport"))));
        verify(expoPushService).sendToUser(eq(30L), eq("Your shared journey is confirmed"), contains("Gate 1"), anyMap(), eq("default"));

        handler.handle(new SqsEventEnvelope("m3", "MOBILITY_GROUP_LEAVE", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(new GroupLeavePayload(10L, 20L, "airport"))));
        verify(expoPushService).sendToUser(eq(20L), eq("Passenger left the journey"), contains("airport"), anyMap(), eq("default"));

        handler.handle(new SqsEventEnvelope("m4", "MOBILITY_WAITLISTED", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(new WaitlistedPayload(10L, 31L))));
        verify(expoPushService).sendToUser(eq(31L), eq("Added to journey waitlist"), contains("seat opens"), anyMap(), eq("default"));

        handler.handle(new SqsEventEnvelope("m5", "MOBILITY_GROUP_CANCELLED", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(new GroupCancelledPayload(10L, 32L))));
        verify(expoPushService).sendToUser(eq(32L), eq("Journey cancelled"), contains("driver cancelled"), anyMap(), eq("default"));
    }

    @Test
    @DisplayName("EmailDispatchSqsHandler decodes EmailDispatchPayload and invokes EmailService")
    void emailDispatchHandler() throws Exception {
        EmailDispatchSqsHandler handler = new EmailDispatchSqsHandler(emailService, sqsProperties, objectMapper);
        EmailDispatchPayload payload = new EmailDispatchPayload(
                "player@mana.com", "Arjun", "Registration Confirmed", "<p>Welcome!</p>");

        handler.handle(new SqsEventEnvelope("em1", "REGISTRATION_EMAIL", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(payload)));

        verify(emailService).send(argThat(msg ->
                "player@mana.com".equals(msg.to())
                        && "Arjun".equals(msg.toName())
                        && "Registration Confirmed".equals(msg.subject())
                        && "<p>Welcome!</p>".equals(msg.htmlBody())));
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

    @Test
    @DisplayName("PaymentEventsSqsHandler orchestrates payment notifications and confirms sports registration")
    void paymentEventsHandler() throws Exception {
        when(notificationServiceProvider.getIfAvailable()).thenReturn(unifiedNotificationService);

        SportsEventRegistration pendingReg = new SportsEventRegistration();
        pendingReg.setId(501L);
        pendingReg.setStatus(SportsEventRegistration.RegistrationStatus.PENDING);
        when(sportsRegistrationRepository.findFirstByEventIdAndUserIdAndStatus(
                77L, 12L, SportsEventRegistration.RegistrationStatus.PENDING))
                .thenReturn(Optional.of(pendingReg));

        PaymentEventsSqsHandler handler = new PaymentEventsSqsHandler(
                notificationServiceProvider,
                sportsRegistrationRepository,
                groupBuyConfirmationProvider,
                eventPublisher,
                sqsProperties,
                objectMapper
        );

        PaymentNotificationPayload notifPayload = new PaymentNotificationPayload(
                12L, "MANA-123", "CASHFREE", new BigDecimal("500.00"), true, null);
        handler.handle(new SqsEventEnvelope("p1", "PAYMENT_NOTIFICATION", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(notifPayload)));

        verify(unifiedNotificationService).orchestrate(argThat(req ->
                req.getRecipientUserId().equals(12L) && "Payment Successful".equals(req.getTitle())));

        CashfreeBusinessConfirmationPayload confirmPayload = new CashfreeBusinessConfirmationPayload(
                "MANA-123", "CF-999", "UPI", new BigDecimal("500.00"), 12L, "SPORTS_REGISTRATION", 77L);
        handler.handle(new SqsEventEnvelope("p2", "CASHFREE_BUSINESS_CONFIRMATION", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(confirmPayload)));

        assertThat(pendingReg.getStatus()).isEqualTo(SportsEventRegistration.RegistrationStatus.CONFIRMED);
        verify(sportsRegistrationRepository).save(pendingReg);
    }

    @Test
    @DisplayName("EmergencySosSqsHandler dispatches emergency pushes for SOS_TRIGGERED and SOS_ESCALATED")
    void emergencySosHandler() throws Exception {
        EmergencySosSqsHandler handler = new EmergencySosSqsHandler(expoPushService, sqsProperties, objectMapper);

        SosTriggeredPayload triggered = new SosTriggeredPayload(
                901L, "Ramesh", "Fall detected", List.of(101L, 102L), List.of(201L));
        handler.handle(new SqsEventEnvelope("sos1", "SOS_TRIGGERED", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(triggered)));

        verify(expoPushService).sendToUsers(eq(List.of(101L, 102L)), contains("SOS from Ramesh"),
                eq("Fall detected"), anyMap(), eq("emergency"));
        verify(expoPushService).sendToUsers(eq(List.of(201L)), contains("SOS Alert"),
                eq("Fall detected"), anyMap(), eq("emergency"));

        SosEscalatedPayload escalated = new SosEscalatedPayload(901L, "Ramesh", List.of(101L));
        handler.handle(new SqsEventEnvelope("sos2", "SOS_ESCALATED", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(escalated)));

        verify(expoPushService).sendToUsers(eq(List.of(101L)), contains("ESCALATED: SOS from Ramesh"),
                anyString(), anyMap(), eq("emergency"));
    }

    @Test
    @DisplayName("NotificationEventsSqsHandler routes UNIFIED_PUSH_DISPATCH and UNIFIED_WHATSAPP_DISPATCH")
    void unifiedChannelNotificationEventsHandler() throws Exception {
        NotificationEventsSqsHandler handler = new NotificationEventsSqsHandler(
                smsService, sqsProperties, objectMapper, expoPushService, whatsAppProvider);

        UnifiedPushPayload pushPayload = new UnifiedPushPayload(
                44L, "Maintenance Due", "Bill generated for Nov", Map.of("type", "BILLING"), "default");
        handler.handle(new SqsEventEnvelope("u1", "UNIFIED_PUSH_DISPATCH", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(pushPayload)));
        verify(expoPushService).sendToUser(eq(44L), eq("Maintenance Due"),
                eq("Bill generated for Nov"), anyMap(), eq("default"));

        UnifiedWhatsAppPayload waPayload = new UnifiedWhatsAppPayload(
                "+919876543210", "community_alert", Map.of("title", "Water Shutoff", "body", "2PM-4PM"));
        handler.handle(new SqsEventEnvelope("u2", "UNIFIED_WHATSAPP_DISPATCH", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(waPayload)));
        verify(whatsAppProvider).sendTemplateMessage(argThat(req ->
                "+919876543210".equals(req.getRecipientPhone()) && "community_alert".equals(req.getTemplateName())));
    }

    @Test
    @DisplayName("ReportJobSqsHandler delegates BATCH_INVOICE_GENERATION to BillingService")
    void batchInvoiceReportJobHandler() throws Exception {
        when(billingServiceProvider.getIfAvailable()).thenReturn(billingService);

        ReportJobSqsHandler handler = new ReportJobSqsHandler(
                eventReportService, expoPushService, sqsProperties, objectMapper, null);
        // Inject billingServiceProvider via Spring ReflectionTestUtils
        org.springframework.test.util.ReflectionTestUtils.setField(
                handler, "billingServiceProvider", billingServiceProvider);

        handler.handle(new SqsEventEnvelope("b1", "BATCH_INVOICE_GENERATION", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(new BatchInvoicePayload(88L))));

        verify(billingService).executeBatchGenerateInvoices(88L);
    }
}
