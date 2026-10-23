package com.manacommunity.api.groupbuy.repository;

import com.manacommunity.api.groupbuy.entity.GroupDeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface GroupDealRepository extends JpaRepository<GroupDeal, Long> {

    Page<GroupDeal> findByCommunityIdAndStatus(Long communityId, GroupDeal.DealStatus status, Pageable pageable);

    Page<GroupDeal> findByCommunityIdAndStatusAndCategory(Long communityId, GroupDeal.DealStatus status, String category, Pageable pageable);

    @Query("SELECT d FROM GroupDeal d WHERE d.community.id = :communityId AND d.status = 'ACTIVE' " +
           "AND (LOWER(d.title) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(d.category) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<GroupDeal> searchActive(Long communityId, String q, Pageable pageable);

    @Query("SELECT d FROM GroupDeal d WHERE d.community.id = :communityId AND d.status = 'ACTIVE' " +
           "AND (d.currentParticipants * 1.0 / d.targetParticipants) >= 0.7 " +
           "ORDER BY (d.currentParticipants * 1.0 / d.targetParticipants) DESC")
    List<GroupDeal> findAlmostUnlocked(Long communityId, Pageable pageable);

    @Query("SELECT d FROM GroupDeal d WHERE d.community.id = :communityId AND d.status = 'ACTIVE' " +
           "AND d.endDate <= :before ORDER BY d.endDate ASC")
    List<GroupDeal> findEndingSoon(Long communityId, LocalDateTime before, Pageable pageable);

    @Query("SELECT d FROM GroupDeal d WHERE d.community.id = :communityId AND d.status = 'ACTIVE' " +
           "AND d.isFestival = true ORDER BY d.endDate ASC")
    List<GroupDeal> findFestivalDeals(Long communityId, Pageable pageable);

    List<GroupDeal> findByStatusAndEndDateBefore(GroupDeal.DealStatus status, LocalDateTime now);
}
