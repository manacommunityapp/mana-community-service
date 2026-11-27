package com.manacommunity.api.testing.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a consumer's single field expectation in a message contract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractFieldExpectation {
    private String fieldName;
    private String expectedType; // "STRING", "NUMBER", "BOOLEAN", "OBJECT", "ARRAY"
    private boolean required;
    private String regexPattern; // Optional regex validator (e.g., ISO-8601, UUID)
}
