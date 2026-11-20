package com.manacommunity.api.healthcare.repository;

import com.manacommunity.api.healthcare.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReferralRepository extends JpaRepository<Referral, Long> {
    List<Referral> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<Referral> findByCommunityIdAndPatientUserId(Long communityId, Long patientUserId);
    List<Referral> findByCommunityIdAndReferringDoctorId(Long communityId, Long doctorId);
    List<Referral> findByCommunityIdAndStatus(Long communityId, String status);
}
