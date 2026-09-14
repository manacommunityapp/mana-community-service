package com.manacommunity.api.repository.karate;

import com.manacommunity.api.model.karate.SportsKarateBelt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsKarateBeltRepository extends JpaRepository<SportsKarateBelt, Long> {

    List<SportsKarateBelt> findByCommunityIdAndActiveTrueOrderByRankAsc(Long communityId);

    List<SportsKarateBelt> findBySportIdAndActiveTrueOrderByRankAsc(Long sportId);

    Optional<SportsKarateBelt> findByCommunityIdAndRank(Long communityId, Integer rank);

    boolean existsByCommunityIdAndRank(Long communityId, Integer rank);

    /** Next belt level above the given rank for a community. */
    Optional<SportsKarateBelt> findFirstByCommunityIdAndRankGreaterThanAndActiveTrueOrderByRankAsc(
            Long communityId, Integer rank);
}
