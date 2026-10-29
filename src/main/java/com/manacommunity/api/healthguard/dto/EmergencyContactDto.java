package com.manacommunity.api.healthguard.dto;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.ContactRelationship;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContactDto {
    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String email;
    private ContactRelationship relationship;
    private Integer priority;
    private Boolean notifyOnSos;
    private Boolean notifyOnAlert;
    private Boolean consentGiven;
    private Boolean isActive;
}
