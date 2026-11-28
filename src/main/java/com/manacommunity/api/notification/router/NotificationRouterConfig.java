package com.manacommunity.api.notification.router;

import com.manacommunity.api.notification.router.channels.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationRouterConfig {

    @Bean
    public NotificationRouter notificationRouter() {
        NotificationRouter router = new NotificationRouter();
        router.registerChannel(new PushNotificationChannel());
        router.registerChannel(new VoipIntercomChannel());
        router.registerChannel(new WhatsAppChannel());
        router.registerChannel(new SmsChannel());
        return router;
    }
}
