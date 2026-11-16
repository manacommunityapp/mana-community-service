package com.manacommunity.api.hrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UploadDocumentRequest {

    @NotBlank
    @Size(max = 50, message = "Document type must not exceed 50 characters")
    private String documentType;

    @NotBlank
    @Size(max = 200, message = "Document name must not exceed 200 characters")
    private String documentName;

    @Size(max = 1000, message = "File URL must not exceed 1000 characters")
    private String fileUrl;

    @Size(max = 500, message = "File key must not exceed 500 characters")
    private String fileKey;

    private LocalDate expiryDate;
}
