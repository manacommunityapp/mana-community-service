package com.manacommunity.api.guardpayroll.repository;

import com.manacommunity.api.guardpayroll.entity.PatrolComplianceTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatrolComplianceTestRepository extends JpaRepository<PatrolComplianceTest, Long> {}
