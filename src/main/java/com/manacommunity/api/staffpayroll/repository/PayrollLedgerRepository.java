package com.manacommunity.api.staffpayroll.repository;

import com.manacommunity.api.staffpayroll.entity.PayrollLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollLedgerRepository extends JpaRepository<PayrollLedger, Long> {
    List<PayrollLedger> findByStaffIdAndPayrollMonth(Long staffId, String month);
    List<PayrollLedger> findByEmployerFlatNumberAndPayrollMonth(String flat, String month);

    @Query("SELECT p FROM PayrollLedger p WHERE p.status = 'CALCULATED' ORDER BY p.staffName")
    List<PayrollLedger> findPendingPayments();
}
