package com.manacommunity.api.governance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "governance_vault_document")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GovernanceVaultDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 50)
    private String category; // BYLAW, AUDIT, MINUTES, POLICY, CERTIFICATE, OTHER

    @Column(name = "document_url", nullable = false, length = 500)
    private String documentUrl;

    @Column(name = "file_size_kb")
    private Long fileSizeKb;

    @Column(length = 10)
    private String fileType; // PDF, DOCX, XLSX

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
