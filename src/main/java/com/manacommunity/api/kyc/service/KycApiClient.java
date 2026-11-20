package com.manacommunity.api.kyc.service;

/**
 * Pluggable interface for real Aadhaar / DigiLocker API calls.
 * Default implementation stubs the OTP flow for dev/test.
 */
public interface KycApiClient {

    /**
     * Initiates OTP send via Aadhaar authority for the given hashed number.
     * Returns an external transaction reference.
     */
    String initiateAadhaarOtp(String aadhaarHash);

    /**
     * Validates OTP with the external authority.
     * Returns true if accepted.
     */
    boolean verifyAadhaarOtp(String externalRef, String otp);
}
