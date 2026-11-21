package com.manacommunity.api.communityops.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WatchlistEntryRepository extends JpaRepository<WatchlistEntry, Long> {
    List<WatchlistEntry> findByCommunityIdAndActiveTrue(Long communityId);
    List<WatchlistEntry> findByCommunityIdAndIdentifierIgnoreCaseAndActiveTrue(Long communityId, String identifier);
}
