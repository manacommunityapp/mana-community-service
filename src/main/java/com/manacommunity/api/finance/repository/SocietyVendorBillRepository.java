package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.SocietyVendorBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface SocietyVendorBillRepository extends JpaRepository<SocietyVendorBill, Long> {

    List<SocietyVendorBill> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<SocietyVendorBill> findByCommunityIdAndStatusOrderByCreatedAtDesc(
            Long communityId, SocietyVendorBill.BillStatus status);

    @Query("SELECT COALESCE(SUM(b.balanceDue), 0) FROM SocietyVendorBill b " +
           "WHERE b.communityId = :communityId AND b.balanceDue > 0 " +
           "AND b.status NOT IN :excludedStatuses")
    BigDecimal sumPendingPayables(Long communityId,
            java.util.List<SocietyVendorBill.BillStatus> excludedStatuses);
}
