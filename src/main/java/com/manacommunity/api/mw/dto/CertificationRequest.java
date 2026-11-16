package com.manacommunity.api.mw.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CertificationRequest {
    private String name;
    private String issuingBody;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String credentialUrl;
}
