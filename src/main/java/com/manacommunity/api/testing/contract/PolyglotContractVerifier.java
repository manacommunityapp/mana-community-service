package com.manacommunity.api.testing.contract;

import java.util.List;
import java.util.Map;

/**
 * Verifies Kafka provider payloads against consumer contracts registered in the Pact Broker.
 */
public interface PolyglotContractVerifier {

    /**
     * Verifies a generated provider message payload against all registered consumer contracts.
     *
     * @param providerName The name of the publishing service
     * @param payloadJson  The JSON payload emitted by the provider
     * @param contracts    The list of consumer contracts pulled from Pact Broker
     * @return List of validation results per consumer
     */
    List<ContractValidationResult> verifyPayload(String providerName, String payloadJson, List<PactMessageContract> contracts);

    /**
     * Determines whether deployment should proceed based on the 'can-i-deploy' matrix.
     *
     * @param results List of verification results
     * @return true if all consumer contracts are satisfied without errors
     */
    boolean canDeploy(List<ContractValidationResult> results);
}
