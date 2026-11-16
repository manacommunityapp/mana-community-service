package com.manacommunity.api.groupbuying.repository;

import com.manacommunity.api.groupbuying.model.DealStatus;
import com.manacommunity.api.groupbuying.model.GroupDeal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupDealRepository extends JpaRepository<GroupDeal, Long> {
    List<GroupDeal> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<GroupDeal> findByCommunityIdAndIsAlmostUnlockedTrue(Long communityId);
    List<GroupDeal> findByCommunityIdAndIsTrendingTrue(Long communityId);
    List<GroupDeal> findByCommunityIdAndIsFestivalDealTrue(Long communityId);
    List<GroupDeal> findByVendorIdOrderByCreatedAtDesc(String vendorId);
    List<GroupDeal> findByDealStatusInAndDealEndsAtBefore(List<DealStatus> statuses, LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM GroupDeal d WHERE d.id = :id")
    Optional<GroupDeal> findByIdForUpdate(@Param("id") Long id);
}
