package com.manacommunity.api.recovery.repository;

import com.manacommunity.api.recovery.entity.BulkMutationGuardrailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BulkMutationGuardrailRepository extends JpaRepository<BulkMutationGuardrailEntity, UUID> {

    List<BulkMutationGuardrailEntity> findBySocietyIdOrderByCreatedAtDesc(Long societyId);
}
