package com.manacommunity.api.serviceplatform.repository;

import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, Long> {

    Optional<ServiceProvider> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Page<ServiceProvider> findByCommunityIdAndVerificationStatus(Long communityId,
                                                                  VerificationStatus status,
                                                                  Pageable pageable);

    Page<ServiceProvider> findByCommunityIdAndVerificationStatusNot(Long communityId,
                                                                     VerificationStatus status,
                                                                     Pageable pageable);
}
