package com.manacommunity.api.hrm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UploadDocumentRequest {

    @NotBlank
    private String documentType;

    @NotBlank
    private String documentName;

    private String fileUrl;
    private String fileKey;
    private LocalDate expiryDate;
}
