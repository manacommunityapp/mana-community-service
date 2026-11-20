package com.manacommunity.api.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "document_access_grant")
@Getter
@Setter
public class DocumentAccessGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "grantee_type", nullable = false, length = 30)
    private String granteeType;

    @Column(name = "grantee_id", nullable = false, length = 100)
    private String granteeId;

    @Column(nullable = false, length = 30)
    private String permission;

    @Column(name = "granted_by")
    private Long grantedBy;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;
}
