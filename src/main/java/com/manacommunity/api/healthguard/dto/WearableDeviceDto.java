package com.manacommunity.api.healthguard.dto;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.WearableType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WearableDeviceDto {
    private Long id;
    private Long userId;
    private WearableType deviceType;
    private String deviceIdentifier;
    private String deviceName;
    private String firmwareVersion;
    private Boolean isActive;
    private LocalDateTime lastSyncAt;
    private LocalDateTime registeredAt;
}
