package com.manacommunity.api.grouporder.repository;

import com.manacommunity.api.grouporder.entity.GroupOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupOrderRepository extends JpaRepository<GroupOrder, Long> {

    Page<GroupOrder> findByCommunityId(Long communityId, Pageable pageable);

    Page<GroupOrder> findByCommunityIdAndStatus(Long communityId, String status, Pageable pageable);

    Page<GroupOrder> findByCommunityIdAndCategory(Long communityId, String category, Pageable pageable);

    Page<GroupOrder> findByCommunityIdAndCategoryAndStatus(
            Long communityId, String category, String status, Pageable pageable);

    List<GroupOrder> findByOrganizerId(Long organizerId);
}
