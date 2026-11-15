package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsAuctionConfig;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsAuctionConfigRepository extends JpaRepository<SportsAuctionConfig, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM SportsAuctionConfig c WHERE c.id = :id")
    Optional<SportsAuctionConfig> findByIdForUpdate(@Param("id") Long id);
    List<SportsAuctionConfig> findBySportIdOrderByCreatedAtDesc(Long sportId);
    @Query("SELECT c FROM SportsAuctionConfig c WHERE c.sport.id = :sportId AND (c.community.id = :communityId OR (c.community IS NULL AND c.createdBy.community.id = :communityId)) ORDER BY c.createdAt DESC")
    List<SportsAuctionConfig> findBySportIdAndCreatedByCommunityIdOrderByCreatedAtDesc(@Param("sportId") Long sportId, @Param("communityId") Long communityId);

    @Query("SELECT c FROM SportsAuctionConfig c WHERE (c.community.id = :communityId OR (c.community IS NULL AND c.createdBy.community.id = :communityId)) ORDER BY c.createdAt DESC")
    List<SportsAuctionConfig> findByCreatedByCommunityIdOrderByCreatedAtDesc(@Param("communityId") Long communityId);

    @Query("SELECT c FROM SportsAuctionConfig c LEFT JOIN FETCH c.sport LEFT JOIN FETCH c.event "
         + "WHERE c.sport.id = :sportId ORDER BY c.createdAt DESC")
    List<SportsAuctionConfig> findBySportIdWithAssociations(@Param("sportId") Long sportId);

    @Query("SELECT c FROM SportsAuctionConfig c LEFT JOIN FETCH c.sport LEFT JOIN FETCH c.event "
         + "WHERE c.sport.id = :sportId AND (c.community.id = :communityId OR (c.community IS NULL AND c.createdBy.community.id = :communityId)) ORDER BY c.createdAt DESC")
    List<SportsAuctionConfig> findBySportIdAndCommunityIdWithAssociations(@Param("sportId") Long sportId, @Param("communityId") Long communityId);

    @Query("SELECT c FROM SportsAuctionConfig c LEFT JOIN FETCH c.sport LEFT JOIN FETCH c.event "
         + "WHERE (c.community.id = :communityId OR (c.community IS NULL AND c.createdBy.community.id = :communityId)) ORDER BY c.createdAt DESC")
    List<SportsAuctionConfig> findByCommunityIdWithAssociations(@Param("communityId") Long communityId);
    Optional<SportsAuctionConfig> findBySportIdAndStatus(Long sportId, SportsAuctionConfig.AuctionStatus status);
    Optional<SportsAuctionConfig> findFirstByEventIdOrderByIdDesc(Long eventId);

    default Optional<SportsAuctionConfig> findByEventId(Long eventId) {
        return findFirstByEventIdOrderByIdDesc(eventId);
    }
    boolean existsBySportIdAndSeasonName(Long sportId, String seasonName);
}
