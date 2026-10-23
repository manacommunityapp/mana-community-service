package com.manacommunity.api.wellness.dto;

import com.manacommunity.api.wellness.enums.WellnessEnums.AlertType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountabilityAlertDto {
    private Long id;
    private AlertType alertType;
    private Long partnerUserId;
    private LocalDateTime triggeredAt;
    private String message;
    private Boolean isRead;
    private String dataJson;
}
