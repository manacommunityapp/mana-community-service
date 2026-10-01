package com.manacommunity.api.notification.orchestrator;

import com.manacommunity.api.notification.orchestrator.NotificationEnums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class NotificationDtos {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnifiedNotificationRequest {
        @NotNull(message = "Recipient user ID is required")
        private Long recipientUserId;

        @NotNull(message = "Category is required")
        private NotificationCategory category;

        @Builder.Default
        private NotificationPriority priority = NotificationPriority.NORMAL;

        @Builder.Default
        private DeliveryStrategy strategy = DeliveryStrategy.PREFERENCE_BASED;

        private List<NotificationChannel> explicitChannels;

        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "Body is required")
        private String body;

        private Map<String, Object> data;
        private String actionUrl;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnifiedNotificationResult {
        private String notificationId;
        private Long recipientUserId;
        private NotificationCategory category;
        private NotificationPriority priority;
        private List<ChannelDeliveryReport> channelReports;
        private boolean overallDelivered;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelDeliveryReport {
        private NotificationChannel channel;
        private DeliveryStatus status;
        private String providerMessageId;
        private String message;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationPreferenceDto {
        private Long id;
        private NotificationCategory category;
        private NotificationChannel channel;
        private boolean isEnabled;
        private boolean quietHoursEnabled;
        private String quietHoursStart;
        private String quietHoursEnd;
        private String timezone;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationPreferenceUpdateRequest {
        private NotificationCategory category;
        private NotificationChannel channel;
        private Boolean isEnabled;
        private Boolean quietHoursEnabled;
        private String quietHoursStart;
        private String quietHoursEnd;
        private String timezone;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WhatsAppTemplateRequest {
        @NotBlank(message = "Phone number is required")
        private String recipientPhone;

        @NotBlank(message = "Template name is required")
        private String templateName;

        private Map<String, String> parameters;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WhatsAppTemplateResponse {
        private boolean success;
        private String providerMessageId;
        private String status;
        private String error;
    }
}
