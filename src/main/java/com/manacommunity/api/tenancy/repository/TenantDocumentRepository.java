package com.manacommunity.api.tenancy.repository;

import com.manacommunity.api.tenancy.entity.TenantDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TenantDocumentRepository extends JpaRepository<TenantDocument, Long> {

    List<TenantDocument> findByTenantRecordId(Long tenantRecordId);

    List<TenantDocument> findByTenantRecordIdAndDocumentType(Long tenantRecordId, TenantDocument.DocumentType type);

    @Query("SELECT d FROM TenantDocument d WHERE d.documentExpiryDate <= :thresholdDate AND d.expiryReminderSent = false AND d.verificationStatus = 'APPROVED'")
    List<TenantDocument> findExpiringDocuments(LocalDate thresholdDate);

    @Query("SELECT d FROM TenantDocument d WHERE d.verificationStatus = 'MC_REVIEW_PENDING'")
    List<TenantDocument> findPendingMcReview();
}
