package com.manacommunity.api.finops.repository;

import com.manacommunity.api.finops.entity.MediaCompressionAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaCompressionAuditRepository extends JpaRepository<MediaCompressionAuditEntity, UUID> {
    List<MediaCompressionAuditEntity> findBySocietyIdOrderByCreatedAtDesc(Long societyId);
}
