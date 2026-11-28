package com.manacommunity.api.dataprivacy.repository;

import com.manacommunity.api.dataprivacy.entity.SecurityAuditTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityAuditTestRepository extends JpaRepository<SecurityAuditTest, Long> {}
