package com.manacommunity.api.email.builder.repository;

import com.manacommunity.api.email.builder.entity.CustomEmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomEmailTemplateRepository extends JpaRepository<CustomEmailTemplate, Long> {

    List<CustomEmailTemplate> findByCommunityIdOrderByUpdatedAtDesc(Long communityId);

    Optional<CustomEmailTemplate> findByIdAndCommunityId(Long id, Long communityId);

    Optional<CustomEmailTemplate> findByCommunityIdAndTemplateKey(Long communityId, String templateKey);
}
