package com.manacommunity.api.repository;

import com.manacommunity.api.model.EmailTemplateDefinition;
import com.manacommunity.api.model.EmailTemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailTemplateDefinitionRepository extends JpaRepository<EmailTemplateDefinition, Long> {

    List<EmailTemplateDefinition> findByCommunity_IdOrderByTemplateCodeAsc(Long communityId);

    List<EmailTemplateDefinition> findAllByOrderByCommunity_IdAscTemplateCodeAsc();

    Optional<EmailTemplateDefinition> findByIdAndCommunity_Id(Long id, Long communityId);

    Optional<EmailTemplateDefinition> findByCommunity_IdAndTemplateCodeIgnoreCase(Long communityId, String templateCode);

    Optional<EmailTemplateDefinition> findByCommunity_IdAndTemplateCodeIgnoreCaseAndStatus(
            Long communityId,
            String templateCode,
            EmailTemplateStatus status
    );
}
