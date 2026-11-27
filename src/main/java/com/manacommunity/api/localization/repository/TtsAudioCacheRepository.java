package com.manacommunity.api.localization.repository;

import com.manacommunity.api.localization.entity.TtsAudioCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TtsAudioCacheRepository extends JpaRepository<TtsAudioCacheEntity, String> {
}
