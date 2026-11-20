package com.manacommunity.api.healthcare.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "provider_credential")
@Data
public class ProviderCredential {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "provider_id", nullable = false) private Long providerId;
    @Column(name = "credential_type", nullable = false, length = 100) private String credentialType;
    @Column(name = "credential_number", length = 100) private String credentialNumber;
    @Column(name = "issued_by", length = 200) private String issuedBy;
    @Column(name = "issued_date") private LocalDate issuedDate;
    @Column(name = "expiry_date", nullable = false) private LocalDate expiryDate;
    @Column(name = "document_url", length = 1000) private String documentUrl;
    @Column private Boolean verified = false;
    @Column(name = "verified_by") private Long verifiedBy;
    @Column(name = "verified_at") private Instant verifiedAt;
    @Column(name = "alert_sent_30") private Boolean alertSent30 = false;
    @Column(name = "alert_sent_7") private Boolean alertSent7 = false;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
    @Column(name = "updated_at") private Instant updatedAt = Instant.now();
}
