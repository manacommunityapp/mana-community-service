package com.manacommunity.api.localization.repository;

import com.manacommunity.api.localization.entity.VoiceGateEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VoiceGateEntryRepository extends JpaRepository<VoiceGateEntryEntity, UUID> {

    List<VoiceGateEntryEntity> findTop30BySocietyIdOrderByCreatedAtDesc(Long societyId);
}
