package com.manacommunity.api.booking.dto;

import com.manacommunity.api.booking.entity.enums.WaitlistStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WaitlistResponse {

    private Long id;
    private Long resourceId;
    private String resourceName;
    private Long userId;
    private String userName;
    private String requestedDate;
    private String requestedStartTime;
    private String requestedEndTime;
    private WaitlistStatus status;
    private Integer position;
    private String notifiedAt;
    private String expiresAt;
    private String createdAt;
}
