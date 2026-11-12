package com.manacommunity.api.fresh.vendor.dto;

import com.manacommunity.api.fresh.vendor.model.DocumentVerificationStatus;
import com.manacommunity.api.fresh.vendor.model.VendorDocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorDocumentDto {
    private Long id;
    private Long vendorId;
    private String vendorBusinessName;
    private VendorDocumentType documentType;
    private String documentNumber;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String fileKey;
    private String fileUrl;
    private DocumentVerificationStatus status;
    private Long verifiedByUserId;
    private String verifiedByUserName;
    private Instant verifiedAt;
    private String rejectionReason;
}
