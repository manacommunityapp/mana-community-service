package com.cpn.domain.referrals.repository;

import com.cpn.domain.referrals.model.Referral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReferralRepository extends JpaRepository<Referral, UUID> {
    List<Referral> findByReferrerId(UUID referrerId);
    List<Referral> findByStatus(String status);
}
