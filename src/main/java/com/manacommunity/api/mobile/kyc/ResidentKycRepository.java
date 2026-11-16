package com.manacommunity.api.mobile.kyc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResidentKycRepository extends JpaRepository<ResidentKycDocument, Long> {
    List<ResidentKycDocument> findByUserIdOrderBySubmittedAtDesc(Long userId);
}
