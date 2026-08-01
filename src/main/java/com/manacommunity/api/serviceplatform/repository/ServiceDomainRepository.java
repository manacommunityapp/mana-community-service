package com.manacommunity.api.serviceplatform.repository;

import com.manacommunity.api.serviceplatform.entity.ServiceDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceDomainRepository extends JpaRepository<ServiceDomain, Long> {

    List<ServiceDomain> findByCommunityIdAndActiveTrueOrderByDisplayOrderAsc(Long communityId);

    List<ServiceDomain> findByCommunityIdIsNullAndActiveTrueOrderByDisplayOrderAsc();

    Optional<ServiceDomain> findBySlugAndCommunityId(String slug, Long communityId);

    boolean existsBySlugAndCommunityId(String slug, Long communityId);
}
