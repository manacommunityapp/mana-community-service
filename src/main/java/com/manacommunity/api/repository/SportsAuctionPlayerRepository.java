package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsAuctionPlayer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsAuctionPlayerRepository extends JpaRepository<SportsAuctionPlayer, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SportsAuctionPlayer p WHERE p.id = :id")
    Optional<SportsAuctionPlayer> findByIdForUpdate(@Param("id") Long id);

    /** Next player in queue for this auction, excluding team captains and owners */
    @Query("SELECT p FROM SportsAuctionPlayer p WHERE p.config.id = :cid AND p.status = 'QUEUED' " +
           "AND (p.user IS NULL OR (p.user.id NOT IN (SELECT t.captainUser.id FROM SportsAuctionTeam t WHERE t.config.id = :cid AND t.captainUser IS NOT NULL) " +
           "AND p.user.id NOT IN (SELECT t.ownerUser.id FROM SportsAuctionTeam t WHERE t.config.id = :cid AND t.ownerUser IS NOT NULL))) " +
           "ORDER BY p.queueOrder ASC")
    List<SportsAuctionPlayer> findQueuedByConfig(@Param("cid") Long configId);

    /** Player currently being sold, excluding team captains and owners */
    @Query("SELECT p FROM SportsAuctionPlayer p WHERE p.config.id = :cid AND p.status = 'SELLING' " +
           "AND (p.user IS NULL OR (p.user.id NOT IN (SELECT t.captainUser.id FROM SportsAuctionTeam t WHERE t.config.id = :cid AND t.captainUser IS NOT NULL) " +
           "AND p.user.id NOT IN (SELECT t.ownerUser.id FROM SportsAuctionTeam t WHERE t.config.id = :cid AND t.ownerUser IS NOT NULL)))")
    Optional<SportsAuctionPlayer> findSellingPlayer(@Param("cid") Long configId);

    @Query("SELECT COUNT(p) FROM SportsAuctionPlayer p WHERE p.config.id = :cid AND p.status = 'QUEUED' " +
           "AND (p.user IS NULL OR (p.user.id NOT IN (SELECT t.captainUser.id FROM SportsAuctionTeam t WHERE t.config.id = :cid AND t.captainUser IS NOT NULL) " +
           "AND p.user.id NOT IN (SELECT t.ownerUser.id FROM SportsAuctionTeam t WHERE t.config.id = :cid AND t.ownerUser IS NOT NULL)))")
    long countQueuedByConfig(@Param("cid") Long configId);

    @Query("SELECT COUNT(p) FROM SportsAuctionPlayer p WHERE p.config.id = :cid AND p.status = 'SOLD'")
    int countSold(@Param("cid") Long configId);

    @EntityGraph(attributePaths = {"assignedTeam", "user"})
    List<SportsAuctionPlayer> findByConfigIdAndCategoryOrderByQueueOrder(Long configId, String category);
    long countByConfigId(Long configId);
    long countByConfigIdAndStatus(Long configId, SportsAuctionPlayer.PlayerStatus status);

    @EntityGraph(attributePaths = {"assignedTeam", "user"})
    List<SportsAuctionPlayer> findByConfigId(Long configId);

    @EntityGraph(attributePaths = {"assignedTeam", "user"})
    List<SportsAuctionPlayer> findByConfigIdAndStatusOrderByQueueOrderAsc(Long configId, SportsAuctionPlayer.PlayerStatus status);

    @Query("SELECT COALESCE(SUM(p.soldPrice), 0) FROM SportsAuctionPlayer p WHERE (:communityId IS NULL OR p.config.createdBy.community.id = :communityId) AND p.status = com.manacommunity.api.model.SportsAuctionPlayer.PlayerStatus.SOLD")
    long sumSoldPriceByCommunity(@Param("communityId") Long communityId);

    @Query("SELECT COUNT(p) FROM SportsAuctionPlayer p WHERE (:communityId IS NULL OR p.config.createdBy.community.id = :communityId) AND p.status = com.manacommunity.api.model.SportsAuctionPlayer.PlayerStatus.SOLD")
    long countSoldByCommunity(@Param("communityId") Long communityId);
}
