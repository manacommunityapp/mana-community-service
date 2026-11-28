package com.manacommunity.api.freshkitchen.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.manacommunity.api.freshkitchen.model.ExecutionMode;
import com.manacommunity.api.freshkitchen.model.FoodCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasketItemRequest {

    @NotBlank(message = "productId is required")
    @JsonAlias({"id", "comboId"})
    private String productId;

    @JsonAlias({"itemName", "name"})
    private String productName;

    @Min(value = 1, message = "quantity must be at least 1")
    @Builder.Default
    private Integer quantity = 1;

    private BigDecimal unitPrice;

    @JsonAlias({"itemCategory"})
    private FoodCategory category;

    private ExecutionMode executionMode;

    @Builder.Default
    private Boolean mon = true;

    @Builder.Default
    private Boolean tue = true;

    @Builder.Default
    private Boolean wed = true;

    @Builder.Default
    private Boolean thu = true;

    @Builder.Default
    private Boolean fri = true;

    @Builder.Default
    private Boolean sat = true;

    @Builder.Default
    private Boolean sun = true;

    private String daysOfWeek;

    private String specialInstructions;

    private List<String> scheduledDays;

    public void setScheduledDays(List<String> days) {
        this.scheduledDays = days;
        if (days != null) {
            List<String> upper = days.stream().map(String::toUpperCase).toList();
            this.mon = upper.contains("MON");
            this.tue = upper.contains("TUE");
            this.wed = upper.contains("WED");
            this.thu = upper.contains("THU");
            this.fri = upper.contains("FRI");
            this.sat = upper.contains("SAT");
            this.sun = upper.contains("SUN");
        }
    }

    public List<String> getScheduledDays() {
        if (this.scheduledDays != null && !this.scheduledDays.isEmpty()) {
            return this.scheduledDays;
        }
        List<String> days = new ArrayList<>();
        if (Boolean.TRUE.equals(mon)) days.add("MON");
        if (Boolean.TRUE.equals(tue)) days.add("TUE");
        if (Boolean.TRUE.equals(wed)) days.add("WED");
        if (Boolean.TRUE.equals(thu)) days.add("THU");
        if (Boolean.TRUE.equals(fri)) days.add("FRI");
        if (Boolean.TRUE.equals(sat)) days.add("SAT");
        if (Boolean.TRUE.equals(sun)) days.add("SUN");
        return days;
    }
}
