package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsAuctionBid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsAuctionBidRepository extends JpaRepository<SportsAuctionBid, Long> {

    /** Highest bid for a player — used to determine current bid */
    @Query("SELECT MAX(b.bidAmount) FROM SportsAuctionBid b WHERE b.player.id = :pid")
    Optional<Long> findMaxBidForPlayer(@Param("pid") Long playerId);

    /** Full bid history for a player, newest first */
    List<SportsAuctionBid> findByPlayerIdOrderByBidAtDesc(Long playerId);

    /** Last bid entry — tells us which team currently leads */
    @Query("SELECT b FROM SportsAuctionBid b WHERE b.player.id = :pid ORDER BY b.bidAmount DESC")
    List<SportsAuctionBid> findTopBidsForPlayer(@Param("pid") Long playerId, Pageable pageable);
    
    // Existing method mapping
    @Query("SELECT MAX(b.bidAmount) FROM SportsAuctionBid b WHERE b.player.id = :playerId")
    Integer findMaxBidByPlayer(@Param("playerId") Long playerId);
}
