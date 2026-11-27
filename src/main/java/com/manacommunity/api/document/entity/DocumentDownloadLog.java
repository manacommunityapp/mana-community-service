package com.manacommunity.api.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "document_download_log")
@Getter
@Setter
public class DocumentDownloadLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "downloader_user_id")
    private Long downloaderUserId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "downloaded_at", nullable = false)
    private Instant downloadedAt = Instant.now();
}
