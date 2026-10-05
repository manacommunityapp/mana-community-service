package com.manacommunity.api.safety.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityIncidentResponse {
    private Long id;
    private String type;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String location;
    private Long reportedById;
    private String reportedByName;
    private Long assignedToId;
    private String assignedToName;
    private String imageUrl;
    private String resolutionNotes;
    private String resolvedAt;
    private String createdAt;
    private String updatedAt;
}
