// serviceplatform/service/ServiceSearchService.java
package com.manacommunity.api.serviceplatform.service;

import com.manacommunity.api.serviceplatform.dto.response.ServiceSearchResult;
import com.manacommunity.api.serviceplatform.entity.ProviderServiceOffering;
import com.manacommunity.api.serviceplatform.repository.ProviderServiceOfferingRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceSearchService {

    private final ProviderServiceOfferingRepository offeringRepository;
    private final ServiceCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<ServiceSearchResult> search(Long communityId, String query, Long domainId,
                                             Long categoryId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by("provider.avgRating").descending());

        Page<ProviderServiceOffering> offerings;

        if (categoryId != null) {
            offerings = offeringRepository.findVerifiedByCategoryAndCommunity(
                    categoryId, communityId, pageable);
        } else {
            offerings = offeringRepository.findByCategoryIdAndAvailableTrue(
                    categoryId, pageable);
        }

        return offerings.map(this::toSearchResult);
    }

    private ServiceSearchResult toSearchResult(ProviderServiceOffering o) {
        return ServiceSearchResult.builder()
                .offeringId(o.getId())
                .offeringTitle(o.getTitle())
                .offeringDescription(o.getDescription())
                .basePrice(o.getBasePrice())
                .pricingUnit(o.getPricingUnit().name())
                .estimatedDurationMinutes(o.getEstimatedDurationMinutes())
                .providerId(o.getProvider() != null ? o.getProvider().getId() : null)
                .providerName(o.getProvider() != null && o.getProvider().getUser() != null
                        ? o.getProvider().getUser().getFullName() : null)
                .providerType(o.getProvider() != null ? o.getProvider().getProviderType().name() : null)
                .providerRating(o.getProvider() != null ? o.getProvider().getAvgRating() : null)
                .providerTotalJobs(o.getProvider() != null ? o.getProvider().getTotalJobsCompleted() : null)
                .verificationStatus(o.getProvider() != null ? o.getProvider().getVerificationStatus().name() : null)
                .categoryId(o.getCategory() != null ? o.getCategory().getId() : null)
                .categoryName(o.getCategory() != null ? o.getCategory().getName() : null)
                .domainId(o.getCategory() != null && o.getCategory().getDomain() != null
                        ? o.getCategory().getDomain().getId() : null)
                .domainName(o.getCategory() != null && o.getCategory().getDomain() != null
                        ? o.getCategory().getDomain().getName() : null)
                .build();
    }
}
