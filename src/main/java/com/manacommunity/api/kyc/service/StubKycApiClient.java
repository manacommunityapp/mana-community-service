package com.manacommunity.api.kyc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Dev/test stub — replace with real Aadhaar API credentials in production.
 * OTP "123456" always passes; anything else fails.
 */
@Slf4j
@Component
public class StubKycApiClient implements KycApiClient {

    @Override
    public String initiateAadhaarOtp(String aadhaarHash) {
        log.info("[STUB] Aadhaar OTP initiated for hash={}", aadhaarHash);
        return "STUB-REF-" + System.currentTimeMillis();
    }

    @Override
    public boolean verifyAadhaarOtp(String externalRef, String otp) {
        boolean accepted = "123456".equals(otp);
        log.info("[STUB] OTP verify ref={} accepted={}", externalRef, accepted);
        return accepted;
    }
}
