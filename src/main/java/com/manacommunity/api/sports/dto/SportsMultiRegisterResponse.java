package com.manacommunity.api.sports.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SportsMultiRegisterResponse {
    private List<RegisteredItem> registered;
    private List<FailedRegistration> failed;

    @Data
    @Builder
    public static class RegisteredItem {
        private Long registrationId;
        private String playerName;
        private String status;
    }

    @Data
    @Builder
    public static class FailedRegistration {
        private String playerName;
        private Long participantUserId;
        private Long familyMemberId;
        private String reason;
    }
}
