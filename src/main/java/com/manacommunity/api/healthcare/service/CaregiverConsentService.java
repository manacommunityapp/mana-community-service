package com.manacommunity.api.healthcare.service;

import com.manacommunity.api.healthcare.entity.CaregiverConsent;
import com.manacommunity.api.healthcare.repository.CaregiverConsentRepository;
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

    public List<CaregiverConsent> getActiveConsentsForPatient(Long communityId, Long patientUserId) {
        return repo.findByCommunityIdAndPatientUserIdAndRevokedFalse(communityId, patientUserId);
    }

    public List<CaregiverConsent> getActiveConsentsForCaregiver(Long communityId, Long caregiverUserId) {
        return repo.findByCommunityIdAndCaregiverUserIdAndRevokedFalse(communityId, caregiverUserId);
    }
}
