package com.manacommunity.api.serviceplatform.interiors.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="service_provider_credential_audit")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceProviderCredentialAudit {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="credential_id",nullable=false) private ServiceProviderCredential credential;
    @Column(name="actor_user_id",nullable=false) private Long actorUserId;
    @Column(nullable=false,length=24) private String action;
    @Column(name="from_status",length=20) private String fromStatus;
    @Column(name="to_status",nullable=false,length=20) private String toStatus;
    @Column(columnDefinition="TEXT") private String note;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @PrePersist void onCreate(){createdAt=LocalDateTime.now();}
}
