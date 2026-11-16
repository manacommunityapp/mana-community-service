package com.manacommunity.api.visitor.repository;

import com.manacommunity.api.visitor.entity.VisitorBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitorBlacklistRepository extends JpaRepository<VisitorBlacklist, Long> {
    List<VisitorBlacklist> findByCommunityIdAndIsActiveTrueOrderByCreatedAtDesc(Long communityId);
    boolean existsByCommunityIdAndPhoneAndIsActiveTrue(Long communityId, String phone);
}
