package com.manacommunity.api.kyc.repository;

import com.manacommunity.api.kyc.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    List<KycDocument> findByUserId(Long userId);
}
