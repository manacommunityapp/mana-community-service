package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceCommissionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommerceCommissionRuleRepository extends JpaRepository<CommerceCommissionRule, Long> {

    List<CommerceCommissionRule> findByCommunityIdAndActiveTrueOrderByPriorityAsc(Long communityId);

    @Query("SELECT r FROM CommerceCommissionRule r WHERE r.community.id = :communityId AND r.active = true " +
           "AND (r.vendorUser.id = :vendorUserId OR r.vendorUser IS NULL) " +
           "ORDER BY r.priority ASC")
    List<CommerceCommissionRule> findApplicableRules(@Param("communityId") Long communityId,
                                                      @Param("vendorUserId") Long vendorUserId);
}
