package com.manacommunity.api.kyc.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class KycSubmitRequest {
    private String documentType;

    /** SHA-256 of the normalised government ID number (lowercase hex, 64 chars). Never the raw number. */
    private String govtIdNumberHash;

    /** Last 1-4 characters of the government ID number, for masked display only. */
    private String govtIdLast4;

    /** @deprecated Use govtIdNumberHash + govtIdLast4 instead. Raw numbers are rejected by the service. */
    @Deprecated
    private String documentNumber;

    private String frontImageBase64;
    private String backImageBase64;

    @JsonIgnore
    public boolean hasRawDocumentNumber() {
        return documentNumber != null && !documentNumber.isBlank()
                && (govtIdNumberHash == null || govtIdNumberHash.isBlank());
    }
}
