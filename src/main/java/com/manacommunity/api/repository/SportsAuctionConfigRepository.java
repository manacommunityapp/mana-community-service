package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsAuctionConfig;
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
    List<SportsAuctionConfig> findBySportIdAndCreatedByCommunityIdOrderByCreatedAtDesc(Long sportId, Long communityId);
    List<SportsAuctionConfig> findByCreatedByCommunityIdOrderByCreatedAtDesc(Long communityId);

    @Query("SELECT c FROM SportsAuctionConfig c LEFT JOIN FETCH c.sport LEFT JOIN FETCH c.event "
         + "WHERE c.sport.id = :sportId ORDER BY c.createdAt DESC")
    List<SportsAuctionConfig> findBySportIdWithAssociations(@Param("sportId") Long sportId);

    @Query("SELECT c FROM SportsAuctionConfig c LEFT JOIN FETCH c.sport LEFT JOIN FETCH c.event "
         + "WHERE c.sport.id = :sportId AND c.createdBy.community.id = :communityId ORDER BY c.createdAt DESC")
    List<SportsAuctionConfig> findBySportIdAndCommunityIdWithAssociations(@Param("sportId") Long sportId, @Param("communityId") Long communityId);

    @Query("SELECT c FROM SportsAuctionConfig c LEFT JOIN FETCH c.sport LEFT JOIN FETCH c.event "
         + "WHERE c.createdBy.community.id = :communityId ORDER BY c.createdAt DESC")
    List<SportsAuctionConfig> findByCommunityIdWithAssociations(@Param("communityId") Long communityId);
    Optional<SportsAuctionConfig> findBySportIdAndStatus(Long sportId, SportsAuctionConfig.AuctionStatus status);
    Optional<SportsAuctionConfig> findByEventId(Long eventId);
    boolean existsBySportIdAndSeasonName(Long sportId, String seasonName);
}
