package com.manacommunity.api.staffpayroll.repository;

import com.manacommunity.api.staffpayroll.entity.EmployerContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployerContractRepository extends JpaRepository<EmployerContract, Long> {
    List<EmployerContract> findByStaffIdAndStatus(Long staffId, EmployerContract.ContractStatus status);

    @Query("SELECT c FROM EmployerContract c WHERE c.staffId = :staffId AND c.status = 'ACTIVE'")
    List<EmployerContract> findActiveContracts(Long staffId);

    List<EmployerContract> findByEmployerFlatNumberAndEmployerTowerName(String flat, String tower);
}
