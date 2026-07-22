package com.manacommunity.api.repository;

import com.manacommunity.api.model.EmailBuilderTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailBuilderTemplateRepository extends JpaRepository<EmailBuilderTemplate, Long> {

    List<EmailBuilderTemplate> findByCommunity_IdOrderByUpdatedAtDesc(Long communityId);

    Optional<EmailBuilderTemplate> findByIdAndCommunity_Id(Long id, Long communityId);
}
