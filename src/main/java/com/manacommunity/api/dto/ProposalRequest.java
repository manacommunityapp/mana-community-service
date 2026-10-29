package com.manacommunity.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProposalRequest {
    private String title;
    private String description;
    private String category;
    private String type;
    private String estimatedCost;
    private List<String> pros;
    private List<String> cons;
}
