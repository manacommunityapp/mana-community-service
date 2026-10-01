package com.manacommunity.api.notification.orchestrator;

import com.manacommunity.api.email.EmailMessage;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.notification.dto.SendSmsRequest;
import com.manacommunity.api.notification.enums.MessageType;
import com.manacommunity.api.notification.enums.SmsLanguage;
import com.manacommunity.api.notification.enums.SmsPriority;
import com.manacommunity.api.notification.orchestrator.NotificationDtos.*;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.*;
import com.manacommunity.api.notification.orchestrator.engine.NotificationPreferenceEngine;
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
    private final ExpoPushService pushService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final WhatsAppProvider whatsAppProvider;
    private final SimpMessagingTemplate messagingTemplate;

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

        // Default: IN_APP and PUSH
        return List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH);
    }

    private ChannelDeliveryReport dispatchToChannel(AppUser user, UnifiedNotificationRequest request, NotificationChannel channel) {
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
                    pushService.sendToUser(user.getId(), request.getTitle(), request.getBody(), request.getData(), "default");
                    return ChannelDeliveryReport.builder().channel(channel).status(DeliveryStatus.DELIVERED).message("Dispatched to Expo Push").build();
                }
                case EMAIL -> {
                    if (user.getEmail() != null && !user.getEmail().isBlank()) {
                        emailService.send(EmailMessage.builder()
                                .to(user.getEmail())
                                .toName(user.getFullName())
                                .subject(request.getTitle())
                                .htmlBody("<p>" + request.getBody() + "</p>")
                                .build());
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
                        var res = whatsAppProvider.sendTemplateMessage(WhatsAppTemplateRequest.builder()
                                .recipientPhone(user.getPhone())
                                .templateName("community_alert")
                                .parameters(Map.of("title", request.getTitle(), "body", request.getBody()))
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
