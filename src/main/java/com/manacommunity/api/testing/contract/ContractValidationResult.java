package com.manacommunity.api.testing.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of verifying a provider payload against consumer contracts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractValidationResult {
    private String consumerName;
    private String providerName;
    private boolean compliant;
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.compliant = false;
    }
}
