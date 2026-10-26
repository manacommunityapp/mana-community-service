package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cpos_crm_document")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CposCrmDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private CposCrmLead lead;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "file_url", length = 1000, nullable = false)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "uploaded_by", length = 255)
    private String uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
