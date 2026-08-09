package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EventMemberFlowResponse {
    private String residentName;
    private String email;
    private String phone;
    private String flatNumber;
    private String block;
    private String tower;
    private String residentType;
    private String occupancyStatus;
    private boolean flatProfileComplete;
    private int familyMemberCount;
    private int eventRegistrationCount;
    private int activityRegistrationCount;
    private int mealRegistrationCount;
    private List<FeatureStatus> features;

    @Data
    @Builder
    public static class FeatureStatus {
        private String key;
        private String label;
        private boolean available;
        private String status;
    }
}
