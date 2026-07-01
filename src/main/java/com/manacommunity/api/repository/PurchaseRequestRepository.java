package com.manacommunity.api.repository;

import com.manacommunity.api.model.ProcurementStatus;
import com.manacommunity.api.model.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {
    List<PurchaseRequest> findByStatusOrderByCreatedAtDesc(ProcurementStatus status);

    List<PurchaseRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(ProcurementStatus status);
}
