package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VmsContractRepository extends JpaRepository<VmsContract, Long> {
    Page<VmsContract> findByCommunityId(Long communityId, Pageable pageable);
    Page<VmsContract> findByCommunityIdAndStatus(Long communityId, VmsContract.ContractStatus status, Pageable pageable);
    Page<VmsContract> findByVendorId(Long vendorId, Pageable pageable);
    Optional<VmsContract> findByIdAndCommunityId(Long id, Long communityId);
    List<VmsContract> findByEndDateBeforeAndStatus(LocalDate date, VmsContract.ContractStatus status);
}
