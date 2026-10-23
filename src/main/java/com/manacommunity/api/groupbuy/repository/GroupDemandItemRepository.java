package com.manacommunity.api.groupbuy.repository;

import com.manacommunity.api.groupbuy.entity.GroupDemandItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupDemandItemRepository extends JpaRepository<GroupDemandItem, Long> {

    Page<GroupDemandItem> findByCommunityIdAndStatusOrderByUpvotesDesc(
            Long communityId, GroupDemandItem.DemandStatus status, Pageable pageable);

    Page<GroupDemandItem> findByCommunityIdOrderByUpvotesDesc(Long communityId, Pageable pageable);
}
