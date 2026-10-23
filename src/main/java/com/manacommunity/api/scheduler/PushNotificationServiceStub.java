package com.manacommunity.api.scheduler;

import com.manacommunity.api.user.model.AppUser;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Legacy stub — kept for reference only.
 *
 * <p>This class intentionally has <strong>no {@code @Service} annotation</strong>.
 * {@link PushNotificationServiceImpl} (marked {@code @Primary}) is the active
 * bean registered in the Spring context. Removing {@code @Service} here prevents
 * a "expected single matching bean but found 2" startup error.
 *
 * <p>Safe to delete once the team is confident the Expo push integration is stable.
 */
@Slf4j
public class PushNotificationServiceStub implements PushNotificationService {

    @Override
    public void sendBulk(List<AppUser> recipients, String title, String body) {
        log.info("[PUSH STUB] Sending '{}' to {} recipients (no-op — stub is inactive)", title, recipients.size());
    }
}

