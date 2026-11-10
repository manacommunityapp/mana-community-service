package com.manacommunity.api.freshkitchen.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizeComboRequest {

    @NotBlank(message = "comboCode is required")
    private String comboCode;

    @Builder.Default
    private List<CustomizedItemSelection> selectedItems = new ArrayList<>();

    private String specialNotes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomizedItemSelection {
        private String originalProductId;
        private String selectedProductId;
        @Builder.Default
        private Integer quantity = 1;
    }
}
