package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventTicketCategoryMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketCategoryMasterRepository extends JpaRepository<EventTicketCategoryMaster, Long> {

    @Query("SELECT t FROM EventTicketCategoryMaster t WHERE (t.communityId IS NULL OR t.communityId = :communityId) AND t.isActive = true ORDER BY t.displayOrder ASC, t.name ASC")
    List<EventTicketCategoryMaster> findActiveByCommunityIdOrGlobal(@Param("communityId") Long communityId);

    @Query("SELECT t FROM EventTicketCategoryMaster t WHERE t.communityId IS NULL OR t.communityId = :communityId ORDER BY t.displayOrder ASC, t.name ASC")
    List<EventTicketCategoryMaster> findAllByCommunityIdOrGlobal(@Param("communityId") Long communityId);

    boolean existsByCommunityIdAndNameIgnoreCase(Long communityId, String name);

    boolean existsByNameIgnoreCaseAndCommunityIdIsNull(String name);

    Optional<EventTicketCategoryMaster> findFirstByCommunityIdAndNameIgnoreCase(Long communityId, String name);
}
