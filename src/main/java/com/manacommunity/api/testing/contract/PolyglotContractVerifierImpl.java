package com.manacommunity.api.testing.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class PolyglotContractVerifierImpl implements PolyglotContractVerifier {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ContractValidationResult> verifyPayload(String providerName, String payloadJson, List<PactMessageContract> contracts) {
        List<ContractValidationResult> results = new ArrayList<>();

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(payloadJson);
        } catch (Exception e) {
            log.error("Failed to parse provider JSON payload: {}", e.getMessage());
            ContractValidationResult parseError = ContractValidationResult.builder()
                    .providerName(providerName)
                    .consumerName("ALL")
                    .compliant(false)
                    .build();
            parseError.addError("Malformed JSON payload: " + e.getMessage());
            results.add(parseError);
            return results;
        }

        for (PactMessageContract contract : contracts) {
            ContractValidationResult result = ContractValidationResult.builder()
                    .consumerName(contract.getConsumerName())
                    .providerName(providerName)
                    .compliant(true)
                    .build();

            if (contract.getFieldExpectations() != null) {
                for (ContractFieldExpectation field : contract.getFieldExpectations()) {
                    validateField(rootNode, field, result);
                }
            }

            results.add(result);
            if (result.isCompliant()) {
                log.info("Consumer contract for [{}] PASSED verification against [{}]", 
                        contract.getConsumerName(), providerName);
            } else {
                log.warn("Consumer contract for [{}] FAILED verification against [{}]: {}", 
                        contract.getConsumerName(), providerName, result.getErrors());
            }
        }

        return results;
    }

    private void validateField(JsonNode rootNode, ContractFieldExpectation field, ContractValidationResult result) {
        String fieldName = field.getFieldName();
        JsonNode node = rootNode.get(fieldName);

        if (node == null || node.isNull()) {
            if (field.isRequired()) {
                result.addError("Required field [" + fieldName + "] is missing from provider payload");
            }
            return;
        }

        // Type checking
        if (field.getExpectedType() != null) {
            switch (field.getExpectedType().toUpperCase()) {
                case "STRING":
                    if (!node.isTextual()) {
                        result.addError("Field [" + fieldName + "] expected STRING but found " + node.getNodeType());
                    }
                    break;
                case "NUMBER":
                    if (!node.isNumber()) {
                        result.addError("Field [" + fieldName + "] expected NUMBER but found " + node.getNodeType());
                    }
                    break;
                case "BOOLEAN":
                    if (!node.isBoolean()) {
                        result.addError("Field [" + fieldName + "] expected BOOLEAN but found " + node.getNodeType());
                    }
                    break;
                case "ARRAY":
                    if (!node.isArray()) {
                        result.addError("Field [" + fieldName + "] expected ARRAY but found " + node.getNodeType());
                    }
                    break;
                case "OBJECT":
                    if (!node.isObject()) {
                        result.addError("Field [" + fieldName + "] expected OBJECT but found " + node.getNodeType());
                    }
                    break;
                default:
                    break;
            }
        }

        // Regex pattern validation
        if (field.getRegexPattern() != null && node.isTextual()) {
            String value = node.asText();
            if (!Pattern.compile(field.getRegexPattern()).matcher(value).matches()) {
                result.addError("Field [" + fieldName + "] value [" + value + "] does not match regex: " + field.getRegexPattern());
            }
        }
    }

    @Override
    public boolean canDeploy(List<ContractValidationResult> results) {
        if (results == null || results.isEmpty()) {
            return true;
        }
        for (ContractValidationResult r : results) {
            if (!r.isCompliant()) {
                return false;
            }
        }
        return true;
    }
}
