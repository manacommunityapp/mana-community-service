package com.manacommunity.api.healthguard.dto;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.NotificationChannel;
import com.manacommunity.api.healthguard.enums.HealthGuardEnums.NotificationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyNotificationDto {
    private Long id;
    private Long incidentId;
    private Long recipientUserId;
    private String recipientPhone;
    private String recipientName;
    private NotificationChannel channel;
    private NotificationStatus status;
    private String message;
    private LocalDateTime sentAt;
    private LocalDateTime acknowledgedAt;
}
