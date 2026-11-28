package com.manacommunity.api.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QrScanRequest {
    /** The full Ed25519-signed QR token string from the scanned QR code. */
    @NotBlank(message = "token is required")
    private String token;

    /** Gate identifier where the scan occurred, e.g. "MAIN_GATE" or "GATE_B". */
    private String gateId;

    /** Display name of the guard performing the scan. */
    private String guardName;

    /** Base64-encoded visitor photo captured at scan time (optional). */
    private String visitorPhoto;
}
