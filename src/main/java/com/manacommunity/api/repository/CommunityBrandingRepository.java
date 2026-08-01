package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommunityBranding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityBrandingRepository extends JpaRepository<CommunityBranding, Long> {

    Optional<CommunityBranding> findByCommunity_Id(Long communityId);
}
