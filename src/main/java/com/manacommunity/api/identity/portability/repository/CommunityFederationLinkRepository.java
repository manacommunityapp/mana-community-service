package com.manacommunity.api.identity.portability.repository;

import com.manacommunity.api.identity.portability.entity.CommunityFederationLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommunityFederationLinkRepository extends JpaRepository<CommunityFederationLink, Long> {

    @Query("SELECT f FROM CommunityFederationLink f WHERE (f.communityIdA = :communityId OR f.communityIdB = :communityId) AND f.status = 'ACTIVE'")
    List<CommunityFederationLink> findActiveByCommunity(Long communityId);

    @Query("SELECT f FROM CommunityFederationLink f WHERE (f.communityIdA = :communityId OR f.communityIdB = :communityId)")
    List<CommunityFederationLink> findAllByCommunity(Long communityId);

    Optional<CommunityFederationLink> findByIdAndStatus(Long id, String status);
}
