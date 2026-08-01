// src/test/java/com/manacommunity/api/unit/service/serviceplatform/ServiceSearchServiceTest.java
package com.manacommunity.api.unit.service.serviceplatform;

import com.manacommunity.api.serviceplatform.dto.response.ServiceSearchResult;
import com.manacommunity.api.serviceplatform.entity.ProviderServiceOffering;
import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.ServiceDomain;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.enums.PricingUnit;
import com.manacommunity.api.serviceplatform.entity.enums.ProviderType;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import com.manacommunity.api.serviceplatform.repository.ProviderServiceOfferingRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import com.manacommunity.api.serviceplatform.service.ServiceSearchService;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceSearchService")
class ServiceSearchServiceTest {

    @Mock private ProviderServiceOfferingRepository offeringRepository;
    @Mock private ServiceCategoryRepository categoryRepository;

    @InjectMocks private ServiceSearchService searchService;

    @Test
    @DisplayName("searches offerings by category and community")
    void searchesByCategoryAndCommunity() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setFullName("Provider User");

        ServiceProvider provider = ServiceProvider.builder()
                .id(10L).providerType(ProviderType.INDIVIDUAL)
                .verificationStatus(VerificationStatus.VERIFIED)
                .avgRating(BigDecimal.valueOf(4.5)).totalJobsCompleted(20)
                .user(user)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        ServiceDomain domain = ServiceDomain.builder().id(1L).name("Home Services").build();
        ServiceCategory category = ServiceCategory.builder()
                .id(5L).name("Electrician").domain(domain)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        ProviderServiceOffering offering = ProviderServiceOffering.builder()
                .id(20L).provider(provider).category(category)
                .title("Wiring & Repair").basePrice(BigDecimal.valueOf(500))
                .pricingUnit(PricingUnit.FLAT).available(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(offeringRepository.findVerifiedByCategoryAndCommunity(eq(5L), eq(100L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(offering)));

        Page<ServiceSearchResult> results = searchService.search(100L, null, null, 5L, 0, 20);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getOfferingTitle()).isEqualTo("Wiring & Repair");
        assertThat(results.getContent().get(0).getProviderRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
    }
}
