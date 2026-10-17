package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.SocietyVendorBill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SocietyVendorBillRepository extends JpaRepository<SocietyVendorBill, Long> {

    List<SocietyVendorBill> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<SocietyVendorBill> findByCommunityIdAndStatusOrderByCreatedAtDesc(
            Long communityId, SocietyVendorBill.BillStatus status);
}
