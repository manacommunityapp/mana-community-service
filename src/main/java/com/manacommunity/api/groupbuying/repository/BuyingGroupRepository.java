package com.manacommunity.api.groupbuying.repository;

import com.manacommunity.api.groupbuying.model.BuyingGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuyingGroupRepository extends JpaRepository<BuyingGroup, Long> {
    List<BuyingGroup> findByCommunityIdOrderByNameAsc(Long communityId);
}
