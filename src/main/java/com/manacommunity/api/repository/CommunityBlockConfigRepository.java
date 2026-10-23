package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommunityBlockConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityBlockConfigRepository extends JpaRepository<CommunityBlockConfig, Long> {

    /** Returns all block configs for a community ordered alphabetically. */
    List<CommunityBlockConfig> findByCommunityIdOrderByBlockNameAsc(Long communityId);

    /** True if at least one block is already configured for this community. */
    boolean existsByCommunityId(Long communityId);

    /** Lookup a specific block by community + block name (case-insensitive). */
    Optional<CommunityBlockConfig> findByCommunityIdAndBlockNameIgnoreCase(Long communityId, String blockName);

    /** Deletes all block configs for a community. */
    void deleteByCommunityId(Long communityId);
}
