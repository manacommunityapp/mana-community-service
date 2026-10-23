package com.manacommunity.api.repository;

import com.manacommunity.api.model.CposCrmEmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CposCrmEmailTemplateRepository extends JpaRepository<CposCrmEmailTemplate, Long> {

    List<CposCrmEmailTemplate> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    Optional<CposCrmEmailTemplate> findByIdAndCommunityId(Long id, Long communityId);
}
