package com.manacommunity.api.helpdesk.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private Long id;
    private String ticketNumber;
    private String subject;
    private String description;
    private String category;
    private String priority;
    private String status;
    private String adminRemarks;
    private Long raisedById;
    private String raisedByName;
    private Long assignedToId;
    private String assignedToName;
    private Long communityId;
    private String slaDueAt;
    private boolean isEscalated;
    private String escalatedAt;
    private int escalationLevel;
    private Integer satisfactionRating;
    private String feedbackRemarks;
    private boolean residentSignoff;
    private String residentSignoffAt;
    private String attachments;
    private String resolvedAt;
    private String slaStatus;
    private int urgencyScore;
    private String aiClassificationJson;
    private String resolutionNotes;
    private String resolutionProofUrl;
    private String resolutionCode;
    private int reopenCount;
    private String createdAt;
    private String updatedAt;
    private List<CommentDto> comments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentDto {
        private Long id;
        private String message;
        private Long authorId;
        private String authorName;
        private String createdAt;
    }
}
