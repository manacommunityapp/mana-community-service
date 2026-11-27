package com.manacommunity.api.procurement.repository;

import com.manacommunity.api.procurement.entity.ProcAmcContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProcAmcContractRepository extends JpaRepository<ProcAmcContract, Long> {

    List<ProcAmcContract> findByCommunityId(Long communityId);

    List<ProcAmcContract> findByCommunityIdAndStatus(Long communityId, ProcAmcContract.AmcStatus status);

    List<ProcAmcContract> findByCommunityIdAndAssetCategory(Long communityId, String assetCategory);

    @Query("SELECT c FROM ProcAmcContract c WHERE c.communityId = :communityId AND c.status = 'ACTIVE' AND c.endDate <= :alertDate")
    List<ProcAmcContract> findExpiringContracts(Long communityId, LocalDate alertDate);

    @Query("SELECT c FROM ProcAmcContract c WHERE c.communityId = :communityId AND c.status = 'ACTIVE' AND c.warrantyExpiry IS NOT NULL AND c.warrantyExpiry <= :alertDate")
    List<ProcAmcContract> findExpiringWarranties(Long communityId, LocalDate alertDate);
}
