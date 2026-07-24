package com.manacommunity.api.serviceplatform.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final ServiceProviderRepository providerRepository;
    private final ServiceRequestRepository requestRepository;
    private final AuditService auditService;

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> VALID_TRANSITIONS = Map.of(
            WorkOrderStatus.CREATED, Set.of(WorkOrderStatus.SCHEDULED, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.SCHEDULED, Set.of(WorkOrderStatus.EN_ROUTE, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.EN_ROUTE, Set.of(WorkOrderStatus.ARRIVED, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.ARRIVED, Set.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.IN_PROGRESS, Set.of(WorkOrderStatus.PAUSED, WorkOrderStatus.COMPLETED, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.PAUSED, Set.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED)
    );

    @Transactional(readOnly = true)
    public WorkOrderResponse getWorkOrder(Long workOrderId, Long communityId) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", workOrderId));
        if (communityId != null && !wo.getCommunity().getId().equals(communityId)) {
            throw new UnauthorizedActionException("Cannot access work orders from another community");
        }
        return toResponse(wo);
    }

    @Transactional
    public WorkOrderResponse updateStatus(Long workOrderId, Long userId, UpdateWorkOrderStatusRequest req) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", workOrderId));

        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));

        if (!wo.getProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedActionException("Cannot update another provider's work order");
        }

        WorkOrderStatus newStatus = parseEnum(WorkOrderStatus.class, req.getStatus());
        Set<WorkOrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(wo.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from " + wo.getStatus() + " to " + newStatus);
        }

        wo.setStatus(newStatus);

        if (newStatus == WorkOrderStatus.IN_PROGRESS && wo.getActualStart() == null) {
            wo.setActualStart(LocalDateTime.now());
        }
        if (newStatus == WorkOrderStatus.COMPLETED) {
            wo.setActualEnd(LocalDateTime.now());
            wo.setProviderSignoff(true);
            wo.setProviderSignoffAt(LocalDateTime.now());
            if (wo.getServiceRequest() != null) {
                wo.getServiceRequest().setStatus(ServiceRequestStatus.COMPLETED);
                requestRepository.save(wo.getServiceRequest());
            }
        }

        if (req.getNotes() != null) wo.setNotes(req.getNotes());
        if (req.getChecklistItems() != null) wo.setChecklistItems(req.getChecklistItems());
        if (req.getMaterialsUsed() != null) wo.setMaterialsUsed(req.getMaterialsUsed());
        if (req.getBeforePhotos() != null) wo.setBeforePhotos(req.getBeforePhotos());
        if (req.getAfterPhotos() != null) wo.setAfterPhotos(req.getAfterPhotos());

        wo = workOrderRepository.save(wo);
        auditService.record(AuditAction.SERVICE_WORK_ORDER_STATUS_UPDATED, AuditModule.SERVICE_PLATFORM,
                "WorkOrder", String.valueOf(workOrderId));
        return toResponse(wo);
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> listProviderWorkOrders(Long userId, int page, int size) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        return workOrderRepository.findByProviderIdOrderByCreatedAtDesc(provider.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> listAllWorkOrders(Long communityId, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        if (status != null && !status.isBlank()) {
            return workOrderRepository.findByCommunityIdAndStatus(communityId,
                    parseEnum(WorkOrderStatus.class, status), pageable).map(this::toResponse);
        }
        return workOrderRepository.findByCommunityIdOrderByCreatedAtDesc(communityId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public WorkOrderResponse signoffResident(Long workOrderId, Long userId) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", workOrderId));

        if (wo.getServiceRequest() == null
                || !wo.getServiceRequest().getRequester().getId().equals(userId)) {
            throw new UnauthorizedActionException("Only the requesting resident can sign off");
        }

        if (wo.getStatus() != WorkOrderStatus.COMPLETED) {
            throw new IllegalStateException("Work order must be COMPLETED before resident signoff");
        }

        wo.setResidentSignoff(true);
        wo.setResidentSignoffAt(LocalDateTime.now());
        wo = workOrderRepository.save(wo);

        ServiceRequest sr = wo.getServiceRequest();
        sr.setStatus(ServiceRequestStatus.COMPLETED);
        requestRepository.save(sr);

        auditService.record(AuditAction.SERVICE_WORK_ORDER_COMPLETED, AuditModule.SERVICE_PLATFORM,
                "WorkOrder", String.valueOf(workOrderId));
        return toResponse(wo);
    }

    private WorkOrderResponse toResponse(WorkOrder wo) {
        return WorkOrderResponse.builder()
                .id(wo.getId())
                .serviceRequestId(wo.getServiceRequest() != null ? wo.getServiceRequest().getId() : null)
                .providerId(wo.getProvider() != null ? wo.getProvider().getId() : null)
                .providerName(wo.getProvider() != null && wo.getProvider().getUser() != null
                        ? wo.getProvider().getUser().getFullName() : null)
                .status(wo.getStatus().name())
                .scheduledStart(wo.getScheduledStart())
                .scheduledEnd(wo.getScheduledEnd())
                .actualStart(wo.getActualStart())
                .actualEnd(wo.getActualEnd())
                .notes(wo.getNotes())
                .checklistItems(wo.getChecklistItems())
                .materialsUsed(wo.getMaterialsUsed())
                .beforePhotos(wo.getBeforePhotos())
                .afterPhotos(wo.getAfterPhotos())
                .residentSignoff(wo.isResidentSignoff())
                .residentSignoffAt(wo.getResidentSignoffAt())
                .providerSignoff(wo.isProviderSignoff())
                .providerSignoffAt(wo.getProviderSignoffAt())
                .invoiceId(wo.getInvoice() != null ? wo.getInvoice().getId() : null)
                .createdAt(wo.getCreatedAt())
                .updatedAt(wo.getUpdatedAt())
                .build();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + enumClass.getSimpleName() + ": " + value);
        }
    }
}
