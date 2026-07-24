package com.manacommunity.api.vendor.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.dto.WorkOrderRequest;
import com.manacommunity.api.vendor.dto.WorkOrderResponse;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.entity.VmsWorkOrder;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import com.manacommunity.api.vendor.repository.VmsWorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VmsWorkOrderService {

    private final VmsWorkOrderRepository workOrderRepo;
    private final VmsVendorRepository vendorRepo;

    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> getCommunityWorkOrders(Long communityId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return workOrderRepo.findByCommunityIdAndStatus(communityId, VmsWorkOrder.WorkOrderStatus.valueOf(status), pageable)
                    .map(this::toResponse);
        }
        return workOrderRepo.findByCommunityId(communityId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> getVendorWorkOrders(Long vendorId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return workOrderRepo.findByVendorIdAndStatus(vendorId, VmsWorkOrder.WorkOrderStatus.valueOf(status), pageable)
                    .map(this::toResponse);
        }
        return workOrderRepo.findByVendorId(vendorId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse getById(Long id, Long communityId) {
        return toResponse(workOrderRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", id)));
    }

    @Transactional
    public WorkOrderResponse create(WorkOrderRequest req, AppUser createdBy, Community community) {
        String woNumber = "WO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        VmsWorkOrder wo = VmsWorkOrder.builder()
                .workOrderNumber(woNumber)
                .title(req.getTitle())
                .description(req.getDescription())
                .type(VmsWorkOrder.WorkOrderType.valueOf(req.getType()))
                .priority(VmsWorkOrder.Priority.valueOf(req.getPriority()))
                .location(req.getLocation())
                .scheduledDate(req.getScheduledDate())
                .estimatedCost(req.getEstimatedCost())
                .createdBy(createdBy)
                .community(community)
                .build();

        if (req.getVendorId() != null) {
            VmsVendor vendor = vendorRepo.findById(req.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor", req.getVendorId()));
            wo.setVendor(vendor);
            wo.setStatus(VmsWorkOrder.WorkOrderStatus.ASSIGNED);
        }

        return toResponse(workOrderRepo.save(wo));
    }

    @Transactional
    public WorkOrderResponse updateStatus(Long id, String status, Long communityId) {
        VmsWorkOrder wo = workOrderRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", id));
        wo.setStatus(VmsWorkOrder.WorkOrderStatus.valueOf(status));
        return toResponse(workOrderRepo.save(wo));
    }

    @Transactional
    public WorkOrderResponse assignVendor(Long id, Long vendorId, Long communityId) {
        VmsWorkOrder wo = workOrderRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", id));
        VmsVendor vendor = vendorRepo.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", vendorId));
        wo.setVendor(vendor);
        wo.setStatus(VmsWorkOrder.WorkOrderStatus.ASSIGNED);
        return toResponse(workOrderRepo.save(wo));
    }

    private WorkOrderResponse toResponse(VmsWorkOrder wo) {
        return WorkOrderResponse.builder()
                .id(wo.getId())
                .workOrderNumber(wo.getWorkOrderNumber())
                .title(wo.getTitle())
                .description(wo.getDescription())
                .type(wo.getType() != null ? wo.getType().name() : null)
                .priority(wo.getPriority() != null ? wo.getPriority().name() : null)
                .status(wo.getStatus() != null ? wo.getStatus().name() : null)
                .vendor(wo.getVendor() != null ? WorkOrderResponse.VendorRef.builder()
                        .id(wo.getVendor().getId())
                        .businessName(wo.getVendor().getBusinessName())
                        .build() : null)
                .location(wo.getLocation())
                .scheduledDate(wo.getScheduledDate())
                .completedDate(wo.getCompletedDate())
                .estimatedCost(wo.getEstimatedCost())
                .actualCost(wo.getActualCost())
                .completionNotes(wo.getCompletionNotes())
                .communityId(wo.getCommunity() != null ? wo.getCommunity().getId() : null)
                .createdAt(wo.getCreatedAt())
                .updatedAt(wo.getUpdatedAt())
                .build();
    }
}
