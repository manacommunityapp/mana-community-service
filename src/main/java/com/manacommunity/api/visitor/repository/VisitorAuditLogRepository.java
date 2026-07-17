package com.manacommunity.api.visitor.repository;

import com.manacommunity.api.visitor.entity.VisitorAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitorAuditLogRepository extends JpaRepository<VisitorAuditLog, Long> {
    List<VisitorAuditLog> findByVisitorPassIdOrderByTimestampDesc(Long visitorPassId);
    List<VisitorAuditLog> findTop50ByOrderByTimestampDesc();
}
