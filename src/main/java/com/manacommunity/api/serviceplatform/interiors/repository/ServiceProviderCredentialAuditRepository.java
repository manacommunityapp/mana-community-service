package com.manacommunity.api.serviceplatform.interiors.repository;

import com.manacommunity.api.serviceplatform.interiors.entity.ServiceProviderCredentialAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceProviderCredentialAuditRepository extends JpaRepository<ServiceProviderCredentialAudit,Long> {
    List<ServiceProviderCredentialAudit> findByCredentialIdOrderByCreatedAtAsc(Long credentialId);
}
