package com.manacommunity.api.document.repository;

import com.manacommunity.api.document.entity.DocumentAccessGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentAccessGrantRepository extends JpaRepository<DocumentAccessGrant, Long> {

    List<DocumentAccessGrant> findByDocumentId(Long documentId);

    boolean existsByDocumentIdAndGranteeTypeAndGranteeIdAndPermission(
            Long documentId, String granteeType, String granteeId, String permission);
}
