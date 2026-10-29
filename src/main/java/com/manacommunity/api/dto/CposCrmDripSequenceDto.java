package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CposCrmDripSequenceDto {
    private Long id;
    private String name;
    private String triggerStage;
    private boolean active;
    private List<StepDto> steps;

    @Data
    @Builder
    public static class StepDto {
        private Long id;
        private int delayDays;
        private String subject;
        private String bodyHtml;
    }
}
