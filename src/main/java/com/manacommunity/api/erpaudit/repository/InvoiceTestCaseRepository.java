package com.manacommunity.api.erpaudit.repository;

import com.manacommunity.api.erpaudit.entity.InvoiceTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceTestCaseRepository extends JpaRepository<InvoiceTestCase, Long> {}
