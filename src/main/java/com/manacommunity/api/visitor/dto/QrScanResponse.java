package com.manacommunity.api.visitor.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QrScanResponse {

    /**
     * Scan outcome:
     * <ul>
     *   <li>CHECKED_IN          — first valid scan; visitor checked in successfully</li>
     *   <li>ALREADY_CHECKED_IN  — QR replay: this token was already used</li>
     *   <li>REJECTED            — invalid/expired token or wrong community</li>
     *   <li>PASS_NOT_APPROVABLE — pass exists but is in a non-APPROVED status</li>
     * </ul>
     */
    private String result;

    private String message;

    /** Full pass details on CHECKED_IN or ALREADY_CHECKED_IN. */
    private VisitorPassResponse pass;

    /** Timestamp of original check-in for ALREADY_CHECKED_IN replays. */
    private String firstCheckedInAt;
}
