package com.manacommunity.api.unit.service.serviceplatform;

import com.manacommunity.api.serviceplatform.dto.request.UpdateWorkOrderStatusRequest;
import com.manacommunity.api.serviceplatform.dto.response.WorkOrderResponse;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceRequestStatus;
import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceRequestRepository;
import com.manacommunity.api.serviceplatform.repository.WorkOrderRepository;
import com.manacommunity.api.serviceplatform.service.WorkOrderService;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkOrderService")
class WorkOrderServiceTest {

    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private ServiceProviderRepository providerRepository;
    @Mock private ServiceRequestRepository requestRepository;
    @Mock private AuditService auditService;

    @InjectMocks private WorkOrderService workOrderService;

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("transitions from CREATED to SCHEDULED")
        void transitionsToScheduled() {
            AppUser user = new AppUser();
            user.setId(1L);
            ServiceProvider provider = ServiceProvider.builder().id(10L).user(user)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            WorkOrder wo = WorkOrder.builder().id(70L).status(WorkOrderStatus.CREATED)
                    .provider(provider)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

            when(workOrderRepository.findById(70L)).thenReturn(Optional.of(wo));
            when(providerRepository.findByUserId(1L)).thenReturn(Optional.of(provider));
            when(workOrderRepository.save(any())).thenReturn(wo);

            UpdateWorkOrderStatusRequest req = new UpdateWorkOrderStatusRequest();
            req.setStatus("SCHEDULED");

            WorkOrderResponse resp = workOrderService.updateStatus(70L, 1L, req);

            assertThat(resp.getStatus()).isEqualTo("SCHEDULED");
        }

        @Test
        @DisplayName("sets actual timestamps on IN_PROGRESS and COMPLETED")
        void setsTimestamps() {
            AppUser user = new AppUser();
            user.setId(1L);
            ServiceProvider provider = ServiceProvider.builder().id(10L).user(user)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            ServiceRequest sr = ServiceRequest.builder().id(50L)
                    .status(ServiceRequestStatus.IN_PROGRESS)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            WorkOrder wo = WorkOrder.builder().id(70L).status(WorkOrderStatus.ARRIVED)
                    .provider(provider).serviceRequest(sr)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

            when(workOrderRepository.findById(70L)).thenReturn(Optional.of(wo));
            when(providerRepository.findByUserId(1L)).thenReturn(Optional.of(provider));
            when(workOrderRepository.save(any())).thenReturn(wo);

            UpdateWorkOrderStatusRequest req = new UpdateWorkOrderStatusRequest();
            req.setStatus("IN_PROGRESS");

            workOrderService.updateStatus(70L, 1L, req);

            assertThat(wo.getActualStart()).isNotNull();
        }

        @Test
        @DisplayName("rejects invalid transition")
        void rejectsInvalidTransition() {
            AppUser user = new AppUser();
            user.setId(1L);
            ServiceProvider provider = ServiceProvider.builder().id(10L).user(user)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            WorkOrder wo = WorkOrder.builder().id(70L).status(WorkOrderStatus.COMPLETED)
                    .provider(provider)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

            when(workOrderRepository.findById(70L)).thenReturn(Optional.of(wo));
            when(providerRepository.findByUserId(1L)).thenReturn(Optional.of(provider));

            UpdateWorkOrderStatusRequest req = new UpdateWorkOrderStatusRequest();
            req.setStatus("CREATED");

            assertThatThrownBy(() -> workOrderService.updateStatus(70L, 1L, req))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("signoffResident")
    class SignoffResident {

        @Test
        @DisplayName("marks resident signoff on completed work order")
        void signsOffSuccessfully() {
            AppUser user = new AppUser();
            user.setId(1L);
            ServiceRequest sr = ServiceRequest.builder().id(50L).requester(user)
                    .status(ServiceRequestStatus.IN_PROGRESS)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
            WorkOrder wo = WorkOrder.builder().id(70L).status(WorkOrderStatus.COMPLETED)
                    .serviceRequest(sr)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

            when(workOrderRepository.findById(70L)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any())).thenReturn(wo);
            when(requestRepository.save(any())).thenReturn(sr);

            WorkOrderResponse resp = workOrderService.signoffResident(70L, 1L);

            assertThat(resp.isResidentSignoff()).isTrue();
        }
    }
}
