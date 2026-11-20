package com.manacommunity.api.document.repository;

import com.manacommunity.api.document.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    Optional<DocumentVersion> findByDocumentIdAndCurrentTrue(Long documentId);

    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(Long documentId);

    long countByDocumentId(Long documentId);
}
