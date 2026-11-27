package com.manacommunity.api.notification.orchestrator;

import com.manacommunity.api.email.EmailMessage;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.notification.dto.SendSmsRequest;
import com.manacommunity.api.notification.enums.MessageType;
import com.manacommunity.api.notification.enums.SmsLanguage;
import com.manacommunity.api.notification.enums.SmsPriority;
import com.manacommunity.api.notification.orchestrator.NotificationDtos.*;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.*;
import com.manacommunity.api.notification.orchestrator.engine.*;
import com.manacommunity.api.notification.orchestrator.event.DomainEvent;
import com.manacommunity.api.notification.orchestrator.provider.WhatsAppProvider;
import com.manacommunity.api.notification.service.SmsService;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedNotificationServiceImpl implements UnifiedNotificationService {

    private final AppUserRepository userRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationAuditLogRepository auditLogRepository;
    private final NotificationPreferenceEngine preferenceEngine;
    private final NotificationRuleEngine ruleEngine;
    private final NotificationRecipientResolver recipientResolver;
    private final NotificationPriorityResolver priorityResolver;
    private final NotificationRetryFallbackEngine retryFallbackEngine;
    private final ExpoPushService pushService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final WhatsAppProvider whatsAppProvider;
    private final SimpMessagingTemplate messagingTemplate;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.manacommunity.platform.sqs.SqsMessagePublisher sqsMessagePublisher;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.manacommunity.platform.sqs.SqsProperties sqsProperties;

    @Override
    @Transactional
    public List<UnifiedNotificationResult> handleDomainEvent(DomainEvent event) {
        log.info("Processing domain event: {} [id: {}]", event.getEventType(), event.getEventId());

        Optional<NotificationRule> ruleOpt = ruleEngine.findRule(event.getEventType());
        if (ruleOpt.isEmpty()) {
            log.warn("No notification rule found for event type: {}", event.getEventType());
            return Collections.emptyList();
        }

        NotificationRule rule = ruleOpt.get();
        List<Long> recipientIds = recipientResolver.resolveRecipients(event, rule);
        NotificationPriority effectivePriority = priorityResolver.resolvePriority(event, rule);

        String title = ruleEngine.renderTemplate(rule.getTitleTemplate(), event.getPayload());
        String body = ruleEngine.renderTemplate(rule.getBodyTemplate(), event.getPayload());
        String actionUrl = ruleEngine.renderTemplate(rule.getActionUrlTemplate(), event.getPayload());

        List<UnifiedNotificationResult> results = new ArrayList<>();

        for (Long recipientId : recipientIds) {
            UnifiedNotificationRequest req = UnifiedNotificationRequest.builder()
                    .recipientUserId(recipientId)
                    .category(rule.getCategory())
                    .priority(effectivePriority)
                    .strategy(rule.getDefaultStrategy())
                    .explicitChannels(rule.getDefaultChannels())
                    .title(title)
                    .body(body)
                    .data(event.getPayload())
                    .actionUrl(actionUrl)
                    .build();

            results.add(orchestrate(req));
        }

        return results;
    }

    @Override
    @Transactional
    public UnifiedNotificationResult retryDelivery(Long auditLogId) {
        NotificationAuditLog failedLog = auditLogRepository.findById(auditLogId)
                .orElseThrow(() -> new IllegalArgumentException("Audit log not found: " + auditLogId));

        Optional<NotificationChannel> nextChannel = retryFallbackEngine.determineNextFallbackChannel(failedLog.getChannel());
        if (nextChannel.isEmpty()) {
            // Re-attempt same channel
            UnifiedNotificationRequest req = UnifiedNotificationRequest.builder()
                    .recipientUserId(failedLog.getUser().getId())
                    .category(failedLog.getCategory())
                    .priority(failedLog.getPriority())
                    .strategy(DeliveryStrategy.FALLBACK_CASCADE)
                    .explicitChannels(List.of(failedLog.getChannel()))
                    .title(failedLog.getTitle())
                    .body(failedLog.getBody())
                    .build();
            return orchestrate(req);
        }

        UnifiedNotificationRequest fallbackReq = retryFallbackEngine.buildFallbackRequest(failedLog, nextChannel.get());
        return orchestrate(fallbackReq);
    }

    @Override
    public List<NotificationRule> getNotificationRules() {
        return ruleEngine.getAllRules();
    }

    @Override
    @Transactional
    public UnifiedNotificationResult orchestrate(UnifiedNotificationRequest request) {
        String notificationId = "NOTIF-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        AppUser user = userRepository.findById(request.getRecipientUserId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient user not found: " + request.getRecipientUserId()));

        List<NotificationChannel> targetChannels = resolveChannels(request);
        List<ChannelDeliveryReport> reports = new ArrayList<>();
        boolean anyDelivered = false;

        LocalTime currentTime = LocalTime.now();

        for (NotificationChannel channel : targetChannels) {
            Optional<NotificationPreference> userPref = preferenceRepository.findByUserIdAndCategoryAndChannel(
                    user.getId(), request.getCategory(), channel);

            var decision = preferenceEngine.evaluate(
                    request.getCategory(),
                    channel,
                    request.getPriority(),
                    userPref,
                    currentTime
            );

            if (!decision.allowed()) {
                reports.add(ChannelDeliveryReport.builder()
                        .channel(channel)
                        .status(decision.status())
                        .message(decision.reason())
                        .build());

                saveAuditLog(notificationId, user, request, channel, decision.status(), decision.reason(), null);
                continue;
            }

            ChannelDeliveryReport report = dispatchToChannel(user, request, channel);
            reports.add(report);

            if (report.getStatus() == DeliveryStatus.DELIVERED) {
                anyDelivered = true;
            }

            saveAuditLog(notificationId, user, request, channel, report.getStatus(), report.getMessage(), report.getProviderMessageId());

            // If fallback cascade is active and this primary channel succeeded, stop cascade
            if (request.getStrategy() == DeliveryStrategy.FALLBACK_CASCADE && report.getStatus() == DeliveryStatus.DELIVERED) {
                break;
            }
        }

        return UnifiedNotificationResult.builder()
                .notificationId(notificationId)
                .recipientUserId(user.getId())
                .category(request.getCategory())
                .priority(request.getPriority())
                .channelReports(reports)
                .overallDelivered(anyDelivered)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationAuditLog> getAuditLogsForUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    private List<NotificationChannel> resolveChannels(UnifiedNotificationRequest request) {
        if (request.getExplicitChannels() != null && !request.getExplicitChannels().isEmpty()) {
            return request.getExplicitChannels();
        }

        if (request.getStrategy() == DeliveryStrategy.ALL_CHANNELS || request.getPriority() == NotificationPriority.CRITICAL) {
            return List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH, NotificationChannel.SMS, NotificationChannel.WHATSAPP, NotificationChannel.EMAIL);
        }

        if (request.getStrategy() == DeliveryStrategy.FALLBACK_CASCADE) {
            return List.of(NotificationChannel.PUSH, NotificationChannel.WHATSAPP, NotificationChannel.SMS);
        }

        return List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH);
    }

    private ChannelDeliveryReport dispatchToChannel(AppUser user, UnifiedNotificationRequest request, NotificationChannel channel) {
        boolean asyncCapable = request.getStrategy() != DeliveryStrategy.FALLBACK_CASCADE
                && sqsMessagePublisher != null && sqsProperties != null;
        try {
            switch (channel) {
                case IN_APP -> {
                    String dest = "/topic/user/" + user.getId() + "/notifications";
                    Map<String, Object> payload = Map.of(
                            "title", request.getTitle(),
                            "body", request.getBody(),
                            "category", request.getCategory().name(),
                            "actionUrl", request.getActionUrl() != null ? request.getActionUrl() : ""
                    );
                    messagingTemplate.convertAndSend(dest, (Object) payload);
                    return ChannelDeliveryReport.builder().channel(channel).status(DeliveryStatus.DELIVERED).message("Sent via WebSocket STOMP").build();
                }
                case PUSH -> {
                    if (asyncCapable) {
                        var pushPayload = new com.manacommunity.api.notification.sqs.NotificationEventsSqsHandler.UnifiedPushPayload(
                                user.getId(), request.getTitle(), request.getBody(), request.getData(), "default");
                        sqsMessagePublisher.publishAfterCommit(
                                sqsProperties.getQueues().getNotificationEvents(),
                                "UNIFIED_PUSH_DISPATCH",
                                pushPayload,
                                () -> pushService.sendToUser(user.getId(), request.getTitle(), request.getBody(), request.getData(), "default"));
                        return ChannelDeliveryReport.builder().channel(channel).status(DeliveryStatus.DELIVERED).message("Queued for Expo Push via SQS").build();
                    }
                    pushService.sendToUser(user.getId(), request.getTitle(), request.getBody(), request.getData(), "default");
                    return ChannelDeliveryReport.builder().channel(channel).status(DeliveryStatus.DELIVERED).message("Dispatched to Expo Push").build();
                }
                case EMAIL -> {
                    if (user.getEmail() != null && !user.getEmail().isBlank()) {
                        EmailMessage emailMsg = EmailMessage.builder()
                                .to(user.getEmail())
                                .toName(user.getFullName())
                                .subject(request.getTitle())
                                .htmlBody("<p>" + request.getBody() + "</p>")
                                .build();
                        if (asyncCapable) {
                            var emailPayload = new com.manacommunity.api.email.sqs.EmailDispatchSqsHandler.EmailDispatchPayload(
                                    emailMsg.to(), emailMsg.toName(), emailMsg.subject(), emailMsg.htmlBody(), null, null);
                            sqsMessagePublisher.publishAfterCommit(
                                    sqsProperties.getQueues().getEmailDispatch(),
                                    "GENERIC_EMAIL",
                                    emailPayload,
                                    () -> emailService.send(emailMsg));
                            return ChannelDeliveryReport.builder().channel(channel).status(DeliveryStatus.DELIVERED).message("Email queued via SQS for " + user.getEmail()).build();
                        }
                        emailService.send(emailMsg);
                        return ChannelDeliveryReport.builder().channel(channel).status(DeliveryStatus.DELIVERED).message("Email queued for " + user.getEmail()).build();
                    }
                    return ChannelDeliveryReport.builder().channel(channel).status(DeliveryStatus.FAILED).message("No email registered").build();
                }
                case SMS -> {
                    if (user.getPhone() != null && !user.getPhone().isBlank()) {
                        Long smsId = smsService.send(SendSmsRequest.builder()
                                .phoneNumber(user.getPhone())
                                .templateCode("GENERIC_NOTIFICATION")
                                .variables(Map.of("title", request.getTitle(), "body", request.getBody()))
                                .priority(request.getPriority() == NotificationPriority.CRITICAL ? SmsPriority.HIGH : SmsPriority.NORMAL)
                                .messageType(MessageType.TRANSACTIONAL)
                                .language(SmsLanguage.EN)
                                .userId(user.getId())
                                .build());
                        return ChannelDeliveryReport.builder().channel(channel).status(DeliveryStatus.DELIVERED).providerMessageId(String.valueOf(smsId)).message("SMS submitted").build();
                    }
                    return ChannelDeliveryReport.builder().channel(channel).status(DeliveryStatus.FAILED).message("No phone number registered").build();
                }
                case WHATSAPP -> {
                    if (user.getPhone() != null && !user.getPhone().isBlank()) {
                        Map<String, String> params = Map.of("title", request.getTitle(), "body", request.getBody());
                        if (asyncCapable) {
                            var waPayload = new com.manacommunity.api.notification.sqs.NotificationEventsSqsHandler.UnifiedWhatsAppPayload(
                                    user.getPhone(), "community_alert", params);
                            sqsMessagePublisher.publishAfterCommit(
                                    sqsProperties.getQueues().getNotificationEvents(),
                                    "UNIFIED_WHATSAPP_DISPATCH",
                                    waPayload,
                                    () -> whatsAppProvider.sendTemplateMessage(WhatsAppTemplateRequest.builder()
                                            .recipientPhone(user.getPhone())
                                            .templateName("community_alert")
                                            .parameters(params)
                                            .build()));
                            return ChannelDeliveryReport.builder().channel(channel).status(DeliveryStatus.DELIVERED).message("WhatsApp message queued via SQS").build();
                        }
                        var res = whatsAppProvider.sendTemplateMessage(WhatsAppTemplateRequest.builder()
                                .recipientPhone(user.getPhone())
                                .templateName("community_alert")
                                .parameters(params)
                                .build());
                        return ChannelDeliveryReport.builder().channel(channel).status(res.isSuccess() ? DeliveryStatus.DELIVERED : DeliveryStatus.FAILED).providerMessageId(res.getProviderMessageId()).message("WhatsApp message dispatched").build();
                    }
                    return ChannelDeliveryReport.builder().channel(channel).status(DeliveryStatus.FAILED).message("No phone registered").build();
                }
                default -> {
                    return ChannelDeliveryReport.builder().channel(channel).status(DeliveryStatus.FAILED).message("Unsupported channel").build();
                }
            }
        } catch (Exception e) {
            log.error("Failed dispatch to channel {} for user {}: {}", channel, user.getId(), e.getMessage());
            return ChannelDeliveryReport.builder().channel(channel).status(DeliveryStatus.FAILED).message(e.getMessage()).build();
        }
    }

    private void saveAuditLog(String notifId, AppUser user, UnifiedNotificationRequest req, NotificationChannel chan, DeliveryStatus status, String reason, String providerMsgId) {
        NotificationAuditLog auditLog = NotificationAuditLog.builder()
                .notificationId(notifId)
                .user(user)
                .category(req.getCategory())
                .channel(chan)
                .priority(req.getPriority())
                .title(req.getTitle())
                .body(req.getBody())
                .status(status)
                .errorReason(reason)
                .providerMessageId(providerMsgId)
                .deliveredAt(status == DeliveryStatus.DELIVERED ? LocalDateTime.now() : null)
                .build();
        auditLogRepository.save(auditLog);
    }
}
