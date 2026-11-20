package com.manacommunity.api.healthcare.repository;

import com.manacommunity.api.healthcare.entity.CaregiverConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CaregiverConsentRepository extends JpaRepository<CaregiverConsent, Long> {
    List<CaregiverConsent> findByCommunityIdAndPatientUserIdAndRevokedFalse(Long communityId, Long patientUserId);
    List<CaregiverConsent> findByCommunityIdAndCaregiverUserIdAndRevokedFalse(Long communityId, Long caregiverUserId);
}
