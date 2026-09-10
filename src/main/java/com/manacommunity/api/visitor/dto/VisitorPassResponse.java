package com.manacommunity.api.visitor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Public DTO for visitor pass API responses.
 *
 * <p><strong>Privacy rules:</strong>
 * <ul>
 *   <li>OTP is NEVER returned after the initial creation response.</li>
 *   <li>{@code encryptedToken} is NEVER returned (internal implementation detail).</li>
 *   <li>{@code visitorPhone} should be masked by the service layer for security-guard
 *       callers; full for the resident owner and admins.</li>
 * </ul>
 */
@Data
@Builder
public class VisitorPassResponse {

    private Long id;
    private String passCode;
    private String visitorName;
    /** Masked phone for security guards (98****10); full for resident/admin. */
    private String visitorPhone;
    private String vehicleNumber;
    private String purpose;
    private String passType;
    private String status;
    private String expectedAt;
    private String checkedInAt;
    private String checkedOutAt;
    private String flatNumber;
    private Long residentId;
    private String residentName;
    private Long communityId;
    private String createdAt;
    private String otpExpiresAt;
    private String gateIn;
    private String gateOut;
    private String guardIn;
    private String guardOut;
    private String visitorPhoto;

    /**
     * Plain OTP — populated ONLY at pass creation time (create / createWalkIn).
     * This field is always {@code null} on all subsequent reads.
     */
    private String otpOnCreation;
}
