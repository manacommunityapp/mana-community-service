package com.manacommunity.api.scheduler;

import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.service.ExpoPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Replaces PushNotificationServiceStub.
 *
 * This class lives in the scheduler package so it matches the existing
 * interface declared there. It delegates to ExpoPushService for the actual
 * Expo API call.
 *
 * @Primary ensures Spring injects this bean wherever PushNotificationService
 * is required, not the stub.
 *
 * MIGRATION: Delete or remove @Service from PushNotificationServiceStub
 * to avoid "expected single matching bean" startup errors.
 */
@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {

    private final ExpoPushService expoPushService;

    /**
     * Sends a push notification to a list of users.
     * Called by the existing NotificationScheduler.
     *
     * Maps the existing sendBulk(List<AppUser>) contract onto the
     * new ExpoPushService.sendToUsers(List<Long>) implementation.
     */
    @Override
    public void sendBulk(List<AppUser> recipients, String title, String body) {
        if (recipients == null || recipients.isEmpty()) return;

        List<Long> recipientIds = recipients.stream()
                .map(AppUser::getId)
                .collect(Collectors.toList());

        expoPushService.sendToUsers(
            recipientIds,
            title,
            body,
            Map.of("type", "GENERAL"),
            "default"
        );

        log.info("[PUSH] sendBulk: title='{}' recipients={}", title, recipientIds.size());
    }
}
