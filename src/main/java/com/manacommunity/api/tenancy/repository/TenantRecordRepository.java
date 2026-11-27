package com.manacommunity.api.tenancy.repository;

import com.manacommunity.api.tenancy.entity.TenantRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TenantRecordRepository extends JpaRepository<TenantRecord, Long> {

    List<TenantRecord> findByCommunityIdAndStatus(Long communityId, TenantRecord.TenantStatus status);

    List<TenantRecord> findByFlatNumberAndTowerName(String flatNumber, String towerName);

    @Query("SELECT t FROM TenantRecord t WHERE t.status = 'ACTIVE' AND t.leaseEndDate <= :thresholdDate")
    List<TenantRecord> findExpiringLeases(LocalDate thresholdDate);

    @Query("SELECT t FROM TenantRecord t WHERE t.status = 'ACTIVE' AND t.kycStatus = 'EXPIRED'")
    List<TenantRecord> findWithExpiredKyc();

    List<TenantRecord> findByOwnerIdAndStatusNot(Long ownerId, TenantRecord.TenantStatus status);
}
