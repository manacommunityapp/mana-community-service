package com.manacommunity.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiningRsvpResponse {
    private Long id;
    private Long diningEventId;
    private String diningEventTitle;
    private Long userId;
    private String userName;
    private Integer guestCount;
    private String specialRequests;
    private String status;
    private LocalDateTime createdAt;
}
