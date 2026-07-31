package com.manacommunity.api.unit.service.serviceplatform;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.serviceplatform.dto.request.CreateOfferingRequest;
import com.manacommunity.api.serviceplatform.dto.request.RegisterProviderRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceOfferingResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceProviderResponse;
import com.manacommunity.api.serviceplatform.entity.ProviderServiceOffering;
import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.ServiceDomain;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.enums.PricingUnit;
import com.manacommunity.api.serviceplatform.entity.enums.ProviderType;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import com.manacommunity.api.serviceplatform.repository.ProviderServiceOfferingRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.serviceplatform.service.ServiceProviderService;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceProviderService")
class ServiceProviderServiceTest {

    @Mock private ServiceProviderRepository providerRepository;
    @Mock private ProviderServiceOfferingRepository offeringRepository;
    @Mock private ServiceCategoryRepository categoryRepository;
    @Mock private AuditService auditService;

    @InjectMocks private ServiceProviderService providerService;

    private AppUser testUser() {
        AppUser u = new AppUser();
        u.setId(1L);
        u.setFullName("Test User");
        Community c = new Community();
        c.setId(100L);
        u.setCommunity(c);
        return u;
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("registers a new individual provider")
        void registersSuccessfully() {
            AppUser user = testUser();
            when(providerRepository.existsByUserId(1L)).thenReturn(false);
            when(providerRepository.save(any(ServiceProvider.class))).thenAnswer(inv -> {
                ServiceProvider p = inv.getArgument(0);
                p.setId(10L);
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                return p;
            });

            RegisterProviderRequest req = new RegisterProviderRequest();
            req.setProviderType("INDIVIDUAL");
            req.setPhone("9876543210");
            req.setEmail("provider@test.com");
            req.setBio("Experienced plumber");

            ServiceProviderResponse resp = providerService.register(req, user);

            assertThat(resp.getProviderType()).isEqualTo("INDIVIDUAL");
            assertThat(resp.getVerificationStatus()).isEqualTo("PENDING");
            verify(providerRepository).save(any(ServiceProvider.class));
        }

        @Test
        @DisplayName("rejects duplicate registration")
        void rejectsDuplicate() {
            AppUser user = testUser();
            when(providerRepository.existsByUserId(1L)).thenReturn(true);

            RegisterProviderRequest req = new RegisterProviderRequest();
            req.setProviderType("INDIVIDUAL");

            assertThatThrownBy(() -> providerService.register(req, user))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("verifyProvider")
    class VerifyProvider {

        @Test
        @DisplayName("approves a pending provider")
        void approvesProvider() {
            ServiceProvider provider = ServiceProvider.builder()
                    .id(10L).verificationStatus(VerificationStatus.PENDING)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
            when(providerRepository.save(any())).thenReturn(provider);

            ServiceProviderResponse resp = providerService.verifyProvider(10L, "VERIFIED");

            assertThat(resp.getVerificationStatus()).isEqualTo("VERIFIED");
        }
    }

    @Nested
    @DisplayName("createOffering")
    class CreateOffering {

        @Test
        @DisplayName("creates offering for verified provider")
        void createsOfferingSuccessfully() {
            ServiceProvider provider = ServiceProvider.builder()
                    .id(10L).verificationStatus(VerificationStatus.VERIFIED)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            ServiceDomain domain = ServiceDomain.builder().id(1L).name("Home").build();
            ServiceCategory category = ServiceCategory.builder()
                    .id(5L).name("Electrician").domain(domain)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            when(providerRepository.findByUserId(1L)).thenReturn(Optional.of(provider));
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
            when(offeringRepository.save(any(ProviderServiceOffering.class))).thenAnswer(inv -> {
                ProviderServiceOffering o = inv.getArgument(0);
                o.setId(20L);
                o.setCreatedAt(LocalDateTime.now());
                o.setUpdatedAt(LocalDateTime.now());
                return o;
            });

            CreateOfferingRequest req = new CreateOfferingRequest();
            req.setCategoryId(5L);
            req.setTitle("Wiring & Repair");
            req.setBasePrice(BigDecimal.valueOf(500));
            req.setPricingUnit("FLAT");

            ServiceOfferingResponse resp = providerService.createOffering(1L, req);

            assertThat(resp.getTitle()).isEqualTo("Wiring & Repair");
            assertThat(resp.getCategoryName()).isEqualTo("Electrician");
        }
    }
}
