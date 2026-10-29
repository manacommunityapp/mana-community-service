package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProposalResponse {
    private String id;
    private String title;
    private String description;
    private String category;
    private String type;
    private String estimatedCost;
    private String submittedBy;
    private String submittedByFlat;
    private String submittedDate;
    private int supportCount;
    private int supportThreshold;
    private boolean hasUserSupported;
    private String status;
    private List<String> pros;
    private List<String> cons;
    private String committeeNotes;
}
