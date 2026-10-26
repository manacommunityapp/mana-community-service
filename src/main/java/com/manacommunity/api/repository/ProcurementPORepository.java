package com.manacommunity.api.repository;

import com.manacommunity.api.model.POStatus;
import com.manacommunity.api.model.ProcurementPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProcurementPORepository extends JpaRepository<ProcurementPO, Long> {

    List<ProcurementPO> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<ProcurementPO> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, POStatus status);

    Optional<ProcurementPO> findByIdAndCommunityId(Long id, Long communityId);

    @Query("SELECT COALESCE(SUM(p.grandTotal), 0) FROM ProcurementPO p WHERE p.communityId = :communityId AND p.status = 'FULFILLED'")
    BigDecimal sumFulfilledSpend(@Param("communityId") Long communityId);

    long countByCommunityId(Long communityId);
}
