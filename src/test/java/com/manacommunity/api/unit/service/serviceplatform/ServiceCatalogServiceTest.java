package com.manacommunity.api.unit.service.serviceplatform;

import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.serviceplatform.dto.request.CreateServiceCategoryRequest;
import com.manacommunity.api.serviceplatform.dto.request.CreateServiceDomainRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceCategoryResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceDomainResponse;
import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.ServiceDomain;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceDomainRepository;
import com.manacommunity.api.serviceplatform.service.ServiceCatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceCatalogService")
class ServiceCatalogServiceTest {

    @Mock
    private ServiceDomainRepository domainRepository;

    @Mock
    private ServiceCategoryRepository categoryRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ServiceCatalogService catalogService;

    @Nested
    @DisplayName("createDomain")
    class CreateDomain {

        @Test
        @DisplayName("creates domain and returns response")
        void createsDomainSuccessfully() {
            CreateServiceDomainRequest req = new CreateServiceDomainRequest();
            req.setName("Home Services");
            req.setSlug("home-services");
            req.setIcon("🏠");
            req.setDescription("All home-related services");
            req.setDisplayOrder(1);

            when(domainRepository.existsBySlugAndCommunityId("home-services", 1L)).thenReturn(false);
            when(domainRepository.save(any(ServiceDomain.class))).thenAnswer(inv -> {
                ServiceDomain d = inv.getArgument(0);
                d.setId(10L);
                d.setCreatedAt(LocalDateTime.now());
                d.setUpdatedAt(LocalDateTime.now());
                return d;
            });

            ServiceDomainResponse resp = catalogService.createDomain(req, 1L);

            assertThat(resp.getName()).isEqualTo("Home Services");
            assertThat(resp.getSlug()).isEqualTo("home-services");
            assertThat(resp.getId()).isEqualTo(10L);
            verify(domainRepository).save(any(ServiceDomain.class));
        }

        @Test
        @DisplayName("rejects duplicate slug within same community")
        void rejectsDuplicateSlug() {
            CreateServiceDomainRequest req = new CreateServiceDomainRequest();
            req.setName("Home Services");
            req.setSlug("home-services");

            when(domainRepository.existsBySlugAndCommunityId("home-services", 1L)).thenReturn(true);

            assertThatThrownBy(() -> catalogService.createDomain(req, 1L))
                    .isInstanceOf(RuntimeException.class);
            verify(domainRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listDomains")
    class ListDomains {

        @Test
        @DisplayName("returns active domains for community")
        void returnsDomainsForCommunity() {
            ServiceDomain d = ServiceDomain.builder()
                    .id(1L).name("Home Services").slug("home-services")
                    .active(true).displayOrder(0)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            when(domainRepository.findByCommunityIdAndActiveTrueOrderByDisplayOrderAsc(1L))
                    .thenReturn(List.of(d));

            List<ServiceDomainResponse> result = catalogService.listDomains(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Home Services");
        }
    }

    @Nested
    @DisplayName("deleteDomain")
    class DeleteDomain {

        @Test
        @DisplayName("soft-deletes domain by setting active=false")
        void softDeletesDomain() {
            ServiceDomain d = ServiceDomain.builder().id(1L).name("Test").active(true)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            when(domainRepository.findById(1L)).thenReturn(Optional.of(d));

            catalogService.deleteDomain(1L);

            assertThat(d.isActive()).isFalse();
            verify(domainRepository).save(d);
        }

        @Test
        @DisplayName("throws when domain not found")
        void throwsWhenNotFound() {
            when(domainRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> catalogService.deleteDomain(99L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategory {

        @Test
        @DisplayName("creates category under domain")
        void createsCategorySuccessfully() {
            ServiceDomain domain = ServiceDomain.builder().id(1L).name("Home Services")
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            when(domainRepository.findById(1L)).thenReturn(Optional.of(domain));
            when(categoryRepository.existsBySlugAndDomainId("electrician", 1L)).thenReturn(false);
            when(categoryRepository.save(any(ServiceCategory.class))).thenAnswer(inv -> {
                ServiceCategory c = inv.getArgument(0);
                c.setId(20L);
                c.setCreatedAt(LocalDateTime.now());
                c.setUpdatedAt(LocalDateTime.now());
                return c;
            });

            CreateServiceCategoryRequest req = new CreateServiceCategoryRequest();
            req.setDomainId(1L);
            req.setName("Electrician");
            req.setSlug("electrician");
            req.setIcon("⚡");

            ServiceCategoryResponse resp = catalogService.createCategory(req);

            assertThat(resp.getName()).isEqualTo("Electrician");
            assertThat(resp.getDomainId()).isEqualTo(1L);
            verify(categoryRepository).save(any(ServiceCategory.class));
        }
    }
}
