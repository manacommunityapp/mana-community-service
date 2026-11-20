package com.manacommunity.api.healthcare.repository;

import com.manacommunity.api.healthcare.entity.ProviderCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface ProviderCredentialRepository extends JpaRepository<ProviderCredential, Long> {
    List<ProviderCredential> findByCommunityIdOrderByExpiryDateAsc(Long communityId);
    List<ProviderCredential> findByCommunityIdAndProviderId(Long communityId, Long providerId);

    @Query("SELECT c FROM ProviderCredential c WHERE c.expiryDate <= :cutoff AND c.alertSent30 = false")
    List<ProviderCredential> findExpiringSoon30(@Param("cutoff") LocalDate cutoff);

    @Query("SELECT c FROM ProviderCredential c WHERE c.expiryDate <= :cutoff AND c.alertSent7 = false")
    List<ProviderCredential> findExpiringSoon7(@Param("cutoff") LocalDate cutoff);
}
