package com.manacommunity.api.fresh.vendor.dto;

import com.manacommunity.api.fresh.vendor.model.VendorDocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadVendorDocumentRequest {
    @NotNull
    private Long vendorId;
    @NotNull
    private VendorDocumentType documentType;
    private String documentNumber;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String fileKey;
    private String fileUrl;
}
