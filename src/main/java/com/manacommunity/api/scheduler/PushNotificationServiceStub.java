package com.manacommunity.api.scheduler;

import com.manacommunity.api.user.model.AppUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Stub implementation of PushNotificationService.
 * Replace with a real Firebase / OneSignal integration.
 */
@Slf4j
@Service
public class PushNotificationServiceStub implements PushNotificationService {

    @Override
    public void sendBulk(List<AppUser> recipients, String title, String body) {
        log.info("[PUSH STUB] Sending '{}' to {} recipients", title, recipients.size());
        // TODO: integrate with FCM / OneSignal
    }
}
