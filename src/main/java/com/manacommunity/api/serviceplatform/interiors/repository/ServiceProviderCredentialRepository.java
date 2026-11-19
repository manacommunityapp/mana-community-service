package com.manacommunity.api.serviceplatform.interiors.repository;

import com.manacommunity.api.serviceplatform.interiors.entity.ServiceProviderCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceProviderCredentialRepository extends JpaRepository<ServiceProviderCredential,Long> {
    List<ServiceProviderCredential> findByProviderIdOrderByCreatedAtDesc(Long providerId);
    List<ServiceProviderCredential> findByStatusOrderByCreatedAtAsc(String status);
    @Query("select c from ServiceProviderCredential c where c.id=:id and c.provider.user.id=:userId")
    Optional<ServiceProviderCredential> findByIdAndProviderUserId(@Param("id") Long id,@Param("userId") Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ServiceProviderCredential c where c.id=:id")
    Optional<ServiceProviderCredential> lockById(@Param("id") Long id);
}
