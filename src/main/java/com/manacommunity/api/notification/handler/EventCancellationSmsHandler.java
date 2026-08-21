package com.manacommunity.api.notification.handler;

import com.manacommunity.api.notification.dto.SendSmsRequest;
import com.manacommunity.api.notification.enums.MessageType;
import com.manacommunity.api.notification.event.EventCancelledEvent;
import com.manacommunity.api.notification.event.EventReminderEvent;
import com.manacommunity.api.notification.service.SmsService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventCancellationSmsHandler {

    private final SmsService smsService;
    private final AppUserRepository userRepository;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventCancelled(EventCancelledEvent event) {
        for (Long userId : event.getAffectedUserIds()) {
            try {
                userRepository.findById(userId).ifPresent(user -> {
                    if (user.getPhone() == null) return;
                    smsService.send(SendSmsRequest.builder()
                            .phoneNumber(user.getPhone())
                            .templateCode("EVENT_CANCELLED")
                            .messageType(MessageType.EVENT)
                            .userId(userId)
                            .variables(Map.of(
                                    "name", user.getFullName(),
                                    "eventName", event.getEventName()))
                            .referenceType("EVENT")
                            .referenceId(event.getEventId())
                            .build());
                });
            } catch (Exception ex) {
                log.warn("EventCancelled SMS failed for userId={}: {}", userId, ex.getMessage());
            }
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventReminder(EventReminderEvent event) {
        try {
            String templateCode = "1h".equals(event.getReminderType())
                    ? "EVENT_REMINDER_1H" : "EVENT_REMINDER_24H";
            smsService.send(SendSmsRequest.builder()
                    .phoneNumber(event.getPhoneNumber())
                    .templateCode(templateCode)
                    .messageType(MessageType.REMINDER)
                    .userId(event.getUserId())
                    .variables(Map.of(
                            "eventName", event.getEventName(),
                            "eventTime", event.getEventTime(),
                            "venue", event.getVenueOrLink()))
                    .build());
        } catch (Exception ex) {
            log.warn("EventReminder SMS failed for userId={}: {}", event.getUserId(), ex.getMessage());
        }
    }
}
