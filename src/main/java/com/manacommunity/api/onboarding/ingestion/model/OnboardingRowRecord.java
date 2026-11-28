package com.manacommunity.api.onboarding.ingestion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingRowRecord {

    public enum RowStatus {
        VALID,
        WARNING,
        ERROR
    }

    private int rowNumber;
    private String rawFlatIdentifier;
    private String normalizedTower;
    private String normalizedFlat;
    private String residentName;
    private String rawPhone;
    private String normalizedPhone;
    private String occupancyType; // OWNER, TENANT, FAMILY_MEMBER
    private String vehicleNumber;
    private String rawOpeningBalance;
    private BigDecimal parsedOpeningBalance;
    private RowStatus status;
    @Builder.Default
    private List<String> validationErrors = new ArrayList<>();
    private String remediationSuggestion;
}
