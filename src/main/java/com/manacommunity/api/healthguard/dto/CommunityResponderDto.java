package com.manacommunity.api.healthguard.dto;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.ResponderAvailability;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommunityResponderDto {
    private Long id;
    private Long userId;
    private Boolean isActive;
    private ResponderAvailability availabilityStatus;
    private String skills;
    private String certifications;
    private String phoneNumber;
    private Integer totalResponseCount;
    private LocalDateTime registeredAt;
    private LocalDateTime lastPingAt;
}
