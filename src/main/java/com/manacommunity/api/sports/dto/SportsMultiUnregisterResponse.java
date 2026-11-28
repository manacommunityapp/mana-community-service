package com.manacommunity.api.sports.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SportsMultiUnregisterResponse {
    private List<Long> withdrawn;
    private List<FailedWithdrawal> failed;

    @Data
    @Builder
    public static class FailedWithdrawal {
        private Long registrationId;
        private String reason;
    }
}
