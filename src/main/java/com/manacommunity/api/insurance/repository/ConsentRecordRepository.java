package com.manacommunity.api.insurance.repository;

import com.manacommunity.api.insurance.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, Long> {
    List<ConsentRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ConsentRecord> findFirstByUserIdAndPurposeAndPartnerIdOrderByIdDesc(Long userId, InsuranceEnums.ConsentPurpose purpose, Long partnerId);
    Optional<ConsentRecord> findFirstByUserIdAndPurposeAndPartnerIdIsNullOrderByIdDesc(Long userId, InsuranceEnums.ConsentPurpose purpose);
}

