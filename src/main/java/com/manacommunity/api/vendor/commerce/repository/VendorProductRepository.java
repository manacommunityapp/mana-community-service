package com.manacommunity.api.vendor.commerce.repository;

import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorProductRepository extends JpaRepository<VendorProduct, Long> {
    List<VendorProduct> findByVendorUserIdOrderByCreatedAtDesc(Long vendorUserId);
    List<VendorProduct> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
}
