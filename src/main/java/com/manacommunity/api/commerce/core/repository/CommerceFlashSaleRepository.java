package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceFlashSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommerceFlashSaleRepository extends JpaRepository<CommerceFlashSale, Long> {

    List<CommerceFlashSale> findByCommunity_IdOrderByStartsAtDesc(Long communityId);

    @Query("SELECT s FROM CommerceFlashSale s WHERE s.community.id = :communityId AND s.status = 'ACTIVE' AND s.startsAt <= :now AND s.endsAt >= :now ORDER BY s.endsAt ASC")
    List<CommerceFlashSale> findActiveSales(@Param("communityId") Long communityId, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM CommerceFlashSale s WHERE s.status = 'SCHEDULED' AND s.startsAt <= :now")
    List<CommerceFlashSale> findSalesReadyToStart(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM CommerceFlashSale s WHERE s.status = 'ACTIVE' AND s.endsAt < :now")
    List<CommerceFlashSale> findExpiredActiveSales(@Param("now") LocalDateTime now);
}
