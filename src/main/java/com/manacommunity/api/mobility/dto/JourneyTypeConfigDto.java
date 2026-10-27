package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class JourneyTypeConfigDto {
    private String code;
    private String label;
    private String description;
    private String icon;
    private List<String> hubExamples;
}
