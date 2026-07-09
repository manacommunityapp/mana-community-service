package com.manacommunity.api.helpdesk.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
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
    private String resolvedAt;
    private String createdAt;
    private String updatedAt;
    private List<CommentDto> comments;

    @Data
    @Builder
    public static class CommentDto {
        private Long id;
        private String message;
        private Long authorId;
        private String authorName;
        private String createdAt;
    }
}
