package com.manacommunity.api.homeservices.repository;

import com.manacommunity.api.homeservices.entity.HomeServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeServiceProviderRepository extends JpaRepository<HomeServiceProvider, Long> {
    List<HomeServiceProvider> findByCommunityIdAndCategoryIdAndIsAvailableTrue(Long communityId, Long categoryId);
    List<HomeServiceProvider> findByCommunityId(Long communityId);
}
