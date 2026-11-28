package com.manacommunity.api.staffpayroll.repository;

import com.manacommunity.api.staffpayroll.entity.SalaryAdvance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryAdvanceRepository extends JpaRepository<SalaryAdvance, Long> {
    List<SalaryAdvance> findByStaffIdAndStatus(Long staffId, SalaryAdvance.AdvanceStatus status);

    @Query("SELECT a FROM SalaryAdvance a WHERE a.contractId = :contractId AND a.status = 'ACTIVE'")
    List<SalaryAdvance> findActiveAdvances(Long contractId);

    @Query("SELECT a FROM SalaryAdvance a WHERE a.status = 'TERMINATED_PENDING'")
    List<SalaryAdvance> findTerminatedPending();
}
