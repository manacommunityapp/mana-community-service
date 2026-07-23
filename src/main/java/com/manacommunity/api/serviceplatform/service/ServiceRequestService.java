package com.manacommunity.api.serviceplatform.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.serviceplatform.dto.request.AssignProviderRequest;
import com.manacommunity.api.serviceplatform.dto.request.CreateServiceRequestDto;
import com.manacommunity.api.serviceplatform.dto.response.ServiceRequestResponse;
import com.manacommunity.api.serviceplatform.dto.response.WorkOrderResponse;
import com.manacommunity.api.serviceplatform.entity.*;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceRequestStatus;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceUrgency;
import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import com.manacommunity.api.serviceplatform.repository.*;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository requestRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final ServiceProviderRepository providerRepository;
    private final ProviderServiceOfferingRepository offeringRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AuditService auditService;

    private static final Set<ServiceRequestStatus> CANCELLABLE = Set.of(
            ServiceRequestStatus.DRAFT, ServiceRequestStatus.SUBMITTED,
            ServiceRequestStatus.MATCHING, ServiceRequestStatus.ASSIGNED);

    @Transactional
    public ServiceRequestResponse createRequest(CreateServiceRequestDto req, AppUser user) {
        ServiceCategory category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", req.getCategoryId()));

        ServiceRequest request = ServiceRequest.builder()
                .requester(user)
                .community(user.getCommunity())
                .category(category)
                .title(req.getTitle())
                .description(req.getDescription())
                .preferredDate(req.getPreferredDate())
                .preferredTimeSlot(req.getPreferredTimeSlot())
                .address(req.getAddress())
                .urgency(req.getUrgency() != null
                        ? parseEnum(ServiceUrgency.class, req.getUrgency())
                        : ServiceUrgency.NORMAL)
                .estimatedCost(req.getEstimatedCost())
                .customFieldValues(req.getCustomFieldValues())
                .attachments(req.getAttachments())
                .status(req.isSubmitImmediately() ? ServiceRequestStatus.SUBMITTED : ServiceRequestStatus.DRAFT)
                .build();

        request = requestRepository.save(request);
        auditService.record(AuditAction.SERVICE_REQUEST_CREATED, AuditModule.SERVICE_PLATFORM,
                "ServiceRequest", String.valueOf(request.getId()));
        return toResponse(request);
    }

    @Transactional
    public ServiceRequestResponse submitRequest(Long requestId, Long userId) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", requestId));

        if (!request.getRequester().getId().equals(userId)) {
            throw new UnauthorizedActionException("Cannot submit another user's request");
        }
        if (request.getStatus() != ServiceRequestStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT requests can be submitted");
        }

        request.setStatus(ServiceRequestStatus.SUBMITTED);
        request = requestRepository.save(request);
        auditService.record(AuditAction.SERVICE_REQUEST_SUBMITTED, AuditModule.SERVICE_PLATFORM,
                "ServiceRequest", String.valueOf(requestId));
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> listMyRequests(Long userId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        return requestRepository.findByRequesterIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse getRequest(Long requestId) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", requestId));
        ServiceRequestResponse resp = toResponse(request);
        workOrderRepository.findByServiceRequestId(requestId)
                .ifPresent(wo -> resp.setWorkOrder(toWorkOrderResponse(wo)));
        return resp;
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> listAllRequests(Long communityId, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        if (status != null && !status.isBlank()) {
            return requestRepository.findByCommunityIdAndStatus(communityId,
                    parseEnum(ServiceRequestStatus.class, status), pageable).map(this::toResponse);
        }
        return requestRepository.findByCommunityIdOrderByCreatedAtDesc(communityId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ServiceRequestResponse assignProvider(Long requestId, AssignProviderRequest req) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", requestId));

        if (request.getStatus() != ServiceRequestStatus.SUBMITTED
                && request.getStatus() != ServiceRequestStatus.MATCHING) {
            throw new IllegalStateException("Request must be in SUBMITTED or MATCHING status to assign");
        }

        ServiceProvider provider = providerRepository.findById(req.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", req.getProviderId()));

        if (provider.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException("Provider must be verified");
        }

        request.setAssignedProvider(provider);
        if (req.getOfferingId() != null) {
            ProviderServiceOffering offering = offeringRepository.findById(req.getOfferingId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProviderServiceOffering", req.getOfferingId()));
            request.setAssignedOffering(offering);
            request.setEstimatedCost(offering.getBasePrice());
        }
        request.setStatus(ServiceRequestStatus.ASSIGNED);
        request = requestRepository.save(request);

        WorkOrder workOrder = WorkOrder.builder()
                .serviceRequest(request)
                .provider(provider)
                .community(request.getCommunity())
                .status(WorkOrderStatus.CREATED)
                .build();
        workOrderRepository.save(workOrder);

        auditService.record(AuditAction.SERVICE_REQUEST_ASSIGNED, AuditModule.SERVICE_PLATFORM,
                "ServiceRequest", String.valueOf(requestId));
        return toResponse(request);
    }

    @Transactional
    public ServiceRequestResponse cancelRequest(Long requestId, Long userId, String reason) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", requestId));

        if (!request.getRequester().getId().equals(userId)) {
            throw new UnauthorizedActionException("Cannot cancel another user's request");
        }

        if (!CANCELLABLE.contains(request.getStatus())) {
            throw new IllegalStateException("Cannot cancel a request in status: " + request.getStatus());
        }

        request.setStatus(ServiceRequestStatus.CANCELLED);
        request.setCancellationReason(reason);
        request = requestRepository.save(request);

        auditService.record(AuditAction.SERVICE_REQUEST_CANCELLED, AuditModule.SERVICE_PLATFORM,
                "ServiceRequest", String.valueOf(requestId));
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> listRequestsForProvider(Long userId, int page, int size) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        return requestRepository.findByAssignedProviderIdAndStatusIn(provider.getId(),
                List.of(ServiceRequestStatus.ASSIGNED, ServiceRequestStatus.IN_PROGRESS), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ServiceRequestResponse acceptRequest(Long requestId, Long userId) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));

        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", requestId));

        if (request.getAssignedProvider() == null) {
            throw new UnauthorizedActionException("This request is not assigned to any provider");
        }

        if (!request.getAssignedProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedActionException("This request is not assigned to you");
        }

        request.setStatus(ServiceRequestStatus.IN_PROGRESS);
        request = requestRepository.save(request);
        return toResponse(request);
    }

    @Transactional
    public ServiceRequestResponse declineRequest(Long requestId, Long userId) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));

        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", requestId));

        if (request.getAssignedProvider() == null) {
            throw new UnauthorizedActionException("This request is not assigned to any provider");
        }

        if (!request.getAssignedProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedActionException("This request is not assigned to you");
        }

        request.setAssignedProvider(null);
        request.setAssignedOffering(null);
        request.setStatus(ServiceRequestStatus.SUBMITTED);
        request = requestRepository.save(request);

        workOrderRepository.findByServiceRequestId(requestId).ifPresent(workOrderRepository::delete);

        return toResponse(request);
    }

    private ServiceRequestResponse toResponse(ServiceRequest r) {
        return ServiceRequestResponse.builder()
                .id(r.getId())
                .requesterId(r.getRequester() != null ? r.getRequester().getId() : null)
                .requesterName(r.getRequester() != null ? r.getRequester().getFullName() : null)
                .categoryId(r.getCategory() != null ? r.getCategory().getId() : null)
                .categoryName(r.getCategory() != null ? r.getCategory().getName() : null)
                .domainName(r.getCategory() != null && r.getCategory().getDomain() != null
                        ? r.getCategory().getDomain().getName() : null)
                .title(r.getTitle())
                .description(r.getDescription())
                .preferredDate(r.getPreferredDate())
                .preferredTimeSlot(r.getPreferredTimeSlot())
                .address(r.getAddress())
                .urgency(r.getUrgency() != null ? r.getUrgency().name() : null)
                .status(r.getStatus().name())
                .assignedProviderId(r.getAssignedProvider() != null ? r.getAssignedProvider().getId() : null)
                .assignedProviderName(r.getAssignedProvider() != null && r.getAssignedProvider().getUser() != null
                        ? r.getAssignedProvider().getUser().getFullName() : null)
                .assignedOfferingId(r.getAssignedOffering() != null ? r.getAssignedOffering().getId() : null)
                .estimatedCost(r.getEstimatedCost())
                .actualCost(r.getActualCost())
                .customFieldValues(r.getCustomFieldValues())
                .attachments(r.getAttachments())
                .cancellationReason(r.getCancellationReason())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private WorkOrderResponse toWorkOrderResponse(WorkOrder wo) {
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
