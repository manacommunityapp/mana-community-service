package com.manacommunity.api.document.repository;

import com.manacommunity.api.document.entity.Document;
import com.manacommunity.api.document.entity.DocumentCategory;
import com.manacommunity.api.document.entity.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByCommunityIdAndStatusNot(Long communityId, DocumentStatus status);

    List<Document> findByEntityTypeAndEntityId(String entityType, String entityId);

    List<Document> findByCommunityIdAndCategory(Long communityId, DocumentCategory category);

    List<Document> findByOwnerUserIdAndCommunityId(Long ownerUserId, Long communityId);

    @Query("SELECT d FROM Document d WHERE d.status = 'ACTIVE' AND d.expiresAt IS NOT NULL AND d.expiresAt <= :deadline")
    List<Document> findExpiringSoon(@Param("deadline") Instant deadline);

    @Query("SELECT d FROM Document d WHERE d.status = 'ACTIVE' AND d.legalHold = false AND d.retentionDays IS NOT NULL AND d.createdAt <= :cutoff")
    List<Document> findPastRetention(@Param("cutoff") Instant cutoff);
}
