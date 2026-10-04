package com.manacommunity.api.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnstileOverrideRequest {
    private String action;
    private Integer durationSeconds;
    private String reason;
}
