package com.manacommunity.api.fresh.vendor.repository;

import com.manacommunity.api.fresh.vendor.model.FreshVendorServiceArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreshVendorServiceAreaRepository extends JpaRepository<FreshVendorServiceArea, Long> {

    List<FreshVendorServiceArea> findByVendorId(Long vendorId);

    List<FreshVendorServiceArea> findByVendorIdAndIsActiveTrue(Long vendorId);

    List<FreshVendorServiceArea> findByCommunityIdAndIsActiveTrue(Long communityId);

    Optional<FreshVendorServiceArea> findByVendorIdAndCommunityId(Long vendorId, Long communityId);
}
