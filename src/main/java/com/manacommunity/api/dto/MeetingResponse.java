package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MeetingResponse {
    private String id;
    private String type;
    private String title;
    private String description;
    private String date;
    private String time;
    private String location;
    private String meetingLink;
    private String status;
    private int quorumConfirmed;
    private int quorumRequired;
    private double quorumPercentage;
    private boolean isQuorumAchieved;
    private boolean hasUserConfirmed;
    private String attendanceMode;
    private String proxyNominee;
    private String annualReportUrl;
    private String minutesUrl;
    private List<AgendaItemResponse> agenda;

    @Data
    @Builder
    public static class AgendaItemResponse {
        private long id;
        private int order;
        private String title;
        private String presenter;
        private String duration;
        private String description;
        private String status;
        private String notes;
        private String attachmentName;
    }
}
