package com.manacommunity.api.watchlist.repository;

import com.manacommunity.api.watchlist.entity.WatchlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WatchlistRepository extends JpaRepository<WatchlistEntry, Long> {
    List<WatchlistEntry> findByCommunityIdAndStatusNot(Long communityId, String status);
}
