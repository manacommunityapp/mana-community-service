package com.manacommunity.api.platform.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditJournalRepository extends JpaRepository<AuditJournalEntry, Long> {

    List<AuditJournalEntry> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<AuditJournalEntry> findByCategoryAndCommunityId(AuditCategory category, Long communityId);

    List<AuditJournalEntry> findByEntityTypeAndEntityId(String entityType, String entityId);
}
