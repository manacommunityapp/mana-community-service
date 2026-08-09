package com.manacommunity.api.events.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ActivityRegistrationResponse {
    private Long id;
    private Long programId;
    private String programTitle;
    private Long eventId;
    private String eventTitle;
    private Long userId;
    private String userName;
    private String userEmail;
    private int headCount;
    private String registrationType;
    private String primaryName;
    private String primaryEmail;
    private String primaryPhone;
    private String status;
    private int spotsLeft;
    private Integer waitlistPosition;
    private String decisionReason;
    private Long approvedById;
    private String approvedByName;
    private String approvedAt;
    private String registeredAt;
    private List<ParticipantResponse> participants;
    private Map<String, Object> customData;

    @Data
    public static class ParticipantResponse {
        private String fullName;
        private Integer age;
        private String gender;
        private String relationship;
        private String email;
        private String phone;
        private Map<String, Object> customData;
    }
}
