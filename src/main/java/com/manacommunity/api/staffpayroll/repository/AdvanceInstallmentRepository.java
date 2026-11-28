package com.manacommunity.api.staffpayroll.repository;

import com.manacommunity.api.staffpayroll.entity.AdvanceInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvanceInstallmentRepository extends JpaRepository<AdvanceInstallment, Long> {
    List<AdvanceInstallment> findByAdvanceIdOrderByInstallmentNumber(Long advanceId);
    List<AdvanceInstallment> findByAdvanceIdAndStatus(Long advanceId, AdvanceInstallment.InstallmentStatus status);
}
