package com.manacommunity.api.intelligence.dto;

import com.manacommunity.api.intelligence.model.ProfileVisibility;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVisibilityRequest {
    @NotNull
    private ProfileVisibility visibility;
    private String bio;
    private List<String> professions;
    private List<String> skills;
    private List<String> interests;
    private List<String> sports;
    private String availabilityHours;
}