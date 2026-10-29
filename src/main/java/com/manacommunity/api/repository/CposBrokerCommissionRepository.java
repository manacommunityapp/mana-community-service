package com.manacommunity.api.repository;

import com.manacommunity.api.model.CposBrokerCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CposBrokerCommissionRepository extends JpaRepository<CposBrokerCommission, Long> {

    List<CposBrokerCommission> findByBrokerIdOrderByCreatedAtDesc(Long brokerId);

    @Query("SELECT bc FROM CposBrokerCommission bc " +
           "WHERE bc.broker.community.id = :communityId " +
           "ORDER BY bc.createdAt DESC")
    List<CposBrokerCommission> findByCommunityId(@Param("communityId") Long communityId);

    @Query("SELECT SUM(bc.commissionLakhs) FROM CposBrokerCommission bc " +
           "WHERE bc.broker.community.id = :communityId AND bc.status = :status")
    BigDecimal sumCommissionByStatus(@Param("communityId") Long communityId, @Param("status") String status);
}
