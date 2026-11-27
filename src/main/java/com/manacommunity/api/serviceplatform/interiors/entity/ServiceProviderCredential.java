package com.manacommunity.api.serviceplatform.interiors.entity;

import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name="service_provider_credential",indexes=@Index(name="idx_provider_credential_provider",columnList="provider_id,created_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceProviderCredential {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="provider_id",nullable=false) private ServiceProvider provider;
    @Column(name="credential_type",nullable=false,length=80) private String credentialType;
    @Column(name="credential_level",nullable=false,length=40) private String credentialLevel;
    @Column(length=180) private String issuer;
    @Column(name="credential_number",length=120) private String credentialNumber;
    @Column(name="evidence_media_id",nullable=false) private UUID evidenceMediaId;
    @Column(nullable=false,length=20) @Builder.Default private String status="PENDING";
    @Column(name="submitted_by",nullable=false) private Long submittedBy;
    @Column(name="reviewed_by") private Long reviewedBy;
    @Column(name="review_note",columnDefinition="TEXT") private String reviewNote;
    @Column(name="reviewed_at") private LocalDateTime reviewedAt;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @PrePersist void onCreate(){createdAt=LocalDateTime.now();}
}
