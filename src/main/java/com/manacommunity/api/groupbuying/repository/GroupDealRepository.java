package com.manacommunity.api.groupbuying.repository;

import com.manacommunity.api.groupbuying.model.GroupDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupDealRepository extends JpaRepository<GroupDeal, Long> {
    List<GroupDeal> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<GroupDeal> findByCommunityIdAndIsAlmostUnlockedTrue(Long communityId);
    List<GroupDeal> findByCommunityIdAndIsTrendingTrue(Long communityId);
    List<GroupDeal> findByCommunityIdAndIsFestivalDealTrue(Long communityId);
}
