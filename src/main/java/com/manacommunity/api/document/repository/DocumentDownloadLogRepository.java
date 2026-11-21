package com.manacommunity.api.document.repository;

import com.manacommunity.api.document.entity.DocumentDownloadLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentDownloadLogRepository extends JpaRepository<DocumentDownloadLog, Long> {

    List<DocumentDownloadLog> findByDocumentIdOrderByDownloadedAtDesc(Long documentId);
}
