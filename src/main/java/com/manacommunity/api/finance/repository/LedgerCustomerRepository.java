package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.LedgerCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerCustomerRepository extends JpaRepository<LedgerCustomer, Long> {
    List<LedgerCustomer> findAllByOrderByNameAsc();
}
