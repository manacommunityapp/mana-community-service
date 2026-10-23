package com.manacommunity.api.repository;

import com.manacommunity.api.model.CposCrmDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CposCrmDocumentRepository extends JpaRepository<CposCrmDocument, Long> {

    List<CposCrmDocument> findByLeadIdAndCommunityIdOrderByCreatedAtDesc(Long leadId, Long communityId);

    Optional<CposCrmDocument> findByIdAndCommunityId(Long id, Long communityId);
}
