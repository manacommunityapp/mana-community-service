package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsVendorCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VmsVendorCategoryRepository extends JpaRepository<VmsVendorCategory, Long> {
    List<VmsVendorCategory> findByCommunityIdAndIsActiveTrue(Long communityId);
    List<VmsVendorCategory> findByCommunityIdAndParentIdIsNullAndIsActiveTrue(Long communityId);
    List<VmsVendorCategory> findByParentIdAndIsActiveTrue(Long parentId);
}
