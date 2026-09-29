package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommunitySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommunitySettingsRepository extends JpaRepository<CommunitySettings, Long> {
    Optional<CommunitySettings> findByCommunityId(Long communityId);
}
