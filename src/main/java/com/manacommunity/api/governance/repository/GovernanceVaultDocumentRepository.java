package com.manacommunity.api.governance.repository;

import com.manacommunity.api.governance.entity.GovernanceVaultDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GovernanceVaultDocumentRepository extends JpaRepository<GovernanceVaultDocument, Long> {
    List<GovernanceVaultDocument> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<GovernanceVaultDocument> findByCommunityIdAndCategoryOrderByCreatedAtDesc(Long communityId, String category);
}
