package com.manacommunity.api.unit.service.serviceplatform;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.serviceplatform.dto.request.AssignProviderRequest;
import com.manacommunity.api.serviceplatform.dto.request.CreateServiceRequestDto;
import com.manacommunity.api.serviceplatform.dto.response.ServiceRequestResponse;
import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.ServiceDomain;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceRequestStatus;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceRequestRepository;
import com.manacommunity.api.serviceplatform.repository.WorkOrderRepository;
import com.manacommunity.api.serviceplatform.service.ServiceRequestService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceRequestService")
class ServiceRequestServiceTest {

    @Mock private ServiceRequestRepository requestRepository;
    @Mock private ServiceCategoryRepository categoryRepository;
    @Mock private ServiceProviderRepository providerRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private AuditService auditService;

    @InjectMocks private ServiceRequestService requestService;

    private AppUser testUser() {
        AppUser u = new AppUser();
        u.setId(1L);
        u.setFullName("Resident User");
        Community c = new Community();
        c.setId(100L);
        u.setCommunity(c);
        return u;
    }

    @Nested
    @DisplayName("createRequest")
    class CreateRequest {

        @Test
        @DisplayName("creates a service request as DRAFT")
        void createsAsDraft() {
            ServiceDomain domain = ServiceDomain.builder().id(1L).name("Home Services").build();
            ServiceCategory cat = ServiceCategory.builder().id(5L).name("Electrician").domain(domain)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(cat));
            when(requestRepository.save(any(ServiceRequest.class))).thenAnswer(inv -> {
                ServiceRequest r = inv.getArgument(0);
                r.setId(50L);
                r.setCreatedAt(LocalDateTime.now());
                r.setUpdatedAt(LocalDateTime.now());
                return r;
            });

            CreateServiceRequestDto req = new CreateServiceRequestDto();
            req.setCategoryId(5L);
            req.setTitle("Fix wiring in kitchen");
            req.setPreferredDate(LocalDate.of(2026, 8, 1));
            req.setSubmitImmediately(false);

            ServiceRequestResponse resp = requestService.createRequest(req, testUser());

            assertThat(resp.getStatus()).isEqualTo("DRAFT");
            assertThat(resp.getTitle()).isEqualTo("Fix wiring in kitchen");
        }

        @Test
        @DisplayName("creates and immediately submits when submitImmediately=true")
        void createsAndSubmits() {
            ServiceDomain domain = ServiceDomain.builder().id(1L).name("Home Services").build();
            ServiceCategory cat = ServiceCategory.builder().id(5L).name("Electrician").domain(domain)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(cat));
            when(requestRepository.save(any(ServiceRequest.class))).thenAnswer(inv -> {
                ServiceRequest r = inv.getArgument(0);
                r.setId(50L);
                r.setCreatedAt(LocalDateTime.now());
                r.setUpdatedAt(LocalDateTime.now());
                return r;
            });

            CreateServiceRequestDto req = new CreateServiceRequestDto();
            req.setCategoryId(5L);
            req.setTitle("Fix wiring");
            req.setSubmitImmediately(true);

            ServiceRequestResponse resp = requestService.createRequest(req, testUser());

            assertThat(resp.getStatus()).isEqualTo("SUBMITTED");
        }
    }

    @Nested
    @DisplayName("assignProvider")
    class AssignProvider {

        @Test
        @DisplayName("assigns provider and creates work order")
        void assignsProviderSuccessfully() {
            ServiceRequest request = ServiceRequest.builder()
                    .id(50L).status(ServiceRequestStatus.SUBMITTED)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            ServiceProvider provider = ServiceProvider.builder()
                    .id(10L).verificationStatus(VerificationStatus.VERIFIED)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            when(requestRepository.findById(50L)).thenReturn(Optional.of(request));
            when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
            when(requestRepository.save(any())).thenReturn(request);
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(inv -> {
                WorkOrder w = inv.getArgument(0);
                w.setId(70L);
                w.setCreatedAt(LocalDateTime.now());
                w.setUpdatedAt(LocalDateTime.now());
                return w;
            });

            AssignProviderRequest req = new AssignProviderRequest();
            req.setProviderId(10L);

            ServiceRequestResponse resp = requestService.assignProvider(50L, req);

            assertThat(resp.getStatus()).isEqualTo("ASSIGNED");
            verify(workOrderRepository).save(any(WorkOrder.class));
        }
    }

    @Nested
    @DisplayName("cancelRequest")
    class CancelRequest {

        @Test
        @DisplayName("cancels a submitted request")
        void cancelsSuccessfully() {
            AppUser user = testUser();
            ServiceRequest request = ServiceRequest.builder()
                    .id(50L).requester(user).status(ServiceRequestStatus.SUBMITTED)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            when(requestRepository.findById(50L)).thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenReturn(request);

            ServiceRequestResponse resp = requestService.cancelRequest(50L, 1L, "Changed my mind");

            assertThat(resp.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("rejects cancel for completed request")
        void rejectsCancelForCompleted() {
            AppUser user = testUser();
            ServiceRequest request = ServiceRequest.builder()
                    .id(50L).requester(user).status(ServiceRequestStatus.COMPLETED)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            when(requestRepository.findById(50L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> requestService.cancelRequest(50L, 1L, "reason"))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
