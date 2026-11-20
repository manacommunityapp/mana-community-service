package com.manacommunity.api.healthcare.service;

import com.manacommunity.api.healthcare.entity.ProviderCredential;
import com.manacommunity.api.healthcare.repository.ProviderCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderCredentialService {

    private final ProviderCredentialRepository repo;

    @Transactional
    public ProviderCredential addCredential(ProviderCredential cred) {
        return repo.save(cred);
    }

    @Transactional
    public ProviderCredential verify(Long id, Long verifiedBy) {
        ProviderCredential cred = repo.findById(id).orElseThrow();
        cred.setVerified(true);
        cred.setVerifiedBy(verifiedBy);
        cred.setVerifiedAt(Instant.now());
        cred.setUpdatedAt(Instant.now());
        return repo.save(cred);
    }

    public List<ProviderCredential> getByProvider(Long communityId, Long providerId) {
        return repo.findByCommunityIdAndProviderId(communityId, providerId);
    }

    public List<ProviderCredential> getByCommunity(Long communityId) {
        return repo.findByCommunityIdOrderByExpiryDateAsc(communityId);
    }

    /** Runs daily at 8am — marks alert flags on expiring credentials. */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkExpiringCredentials() {
        LocalDate today = LocalDate.now();

        List<ProviderCredential> expiring30 = repo.findExpiringSoon30(today.plusDays(30));
        for (ProviderCredential c : expiring30) {
            log.info("Credential expiry 30-day alert: provider={} type={} expiry={}", c.getProviderId(), c.getCredentialType(), c.getExpiryDate());
            c.setAlertSent30(true);
            repo.save(c);
        }

        List<ProviderCredential> expiring7 = repo.findExpiringSoon7(today.plusDays(7));
        for (ProviderCredential c : expiring7) {
            log.info("Credential expiry 7-day alert: provider={} type={} expiry={}", c.getProviderId(), c.getCredentialType(), c.getExpiryDate());
            c.setAlertSent7(true);
            repo.save(c);
        }
    }
}
