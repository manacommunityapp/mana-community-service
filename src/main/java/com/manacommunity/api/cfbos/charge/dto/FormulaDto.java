package com.manacommunity.api.cfbos.charge.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FormulaDto {
    private Long id;
    private String name;
    private String expression;
    private String description;
    private Boolean isActive;
    private List<VariableDto> variables;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class VariableDto {
        private String variableName;
        private String variableSource;
        private String sourceField;
        private String defaultValue;
        private String description;
    }
}
