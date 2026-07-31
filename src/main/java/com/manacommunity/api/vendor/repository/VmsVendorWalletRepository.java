package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsVendorWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VmsVendorWalletRepository extends JpaRepository<VmsVendorWallet, Long> {
    Optional<VmsVendorWallet> findByVendorIdAndCommunityId(Long vendorId, Long communityId);
}
