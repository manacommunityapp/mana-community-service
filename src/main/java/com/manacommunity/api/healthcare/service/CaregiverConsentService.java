package com.manacommunity.api.healthcare.service;

import com.manacommunity.api.healthcare.entity.CaregiverConsent;
import com.manacommunity.api.healthcare.repository.CaregiverConsentRepository;
import com.manacommunity.platform.privacy.HealthDataAccess;
import com.manacommunity.platform.privacy.HealthDataIsolationGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaregiverConsentService {

    private final CaregiverConsentRepository repo;

    @Transactional
    public CaregiverConsent grantConsent(CaregiverConsent consent) {
        return repo.save(consent);
    }

    @Transactional
    public CaregiverConsent revokeConsent(Long id, Long revokedBy) {
        CaregiverConsent c = repo.findById(id).orElseThrow();
        c.setRevoked(true);
        c.setRevokedAt(Instant.now());
        c.setRevokedBy(revokedBy);
        c.setUpdatedAt(Instant.now());
        return repo.save(c);
    }

    @HealthDataAccess(recordType = "CAREGIVER_CONSENT", purpose = "PATIENT_VIEW")
    public List<CaregiverConsent> getActiveConsentsForPatient(Long communityId, Long patientUserId) {
        HealthDataIsolationGuard.assertNotCommunityFeedContext();
        return repo.findByCommunityIdAndPatientUserIdAndRevokedFalse(communityId, patientUserId);
    }

    @HealthDataAccess(recordType = "CAREGIVER_CONSENT", purpose = "CAREGIVER_VIEW")
    public List<CaregiverConsent> getActiveConsentsForCaregiver(Long communityId, Long caregiverUserId) {
        HealthDataIsolationGuard.assertNotCommunityFeedContext();
        return repo.findByCommunityIdAndCaregiverUserIdAndRevokedFalse(communityId, caregiverUserId);
    }
}
