package com.manacommunity.api.groupbuying.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizedCollectorResponse {
    private String orderNumber;
    private String collectorName;
    private String collectorRelation;
    private String collectorPin;
    private String pin;
    private String expiresAt;
    private LocalDateTime authorizedAt;
}
