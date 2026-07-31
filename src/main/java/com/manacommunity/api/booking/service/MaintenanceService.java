package com.manacommunity.api.booking.service;

import com.manacommunity.api.booking.dto.MaintenanceRequest;
import com.manacommunity.api.booking.dto.MaintenanceResponse;
import com.manacommunity.api.booking.entity.Resource;
import com.manacommunity.api.booking.entity.ResourceMaintenance;
import com.manacommunity.api.booking.entity.enums.MaintenanceStatus;
import com.manacommunity.api.booking.entity.enums.MaintenanceType;
import com.manacommunity.api.booking.repository.ResourceMaintenanceRepository;
import com.manacommunity.api.booking.repository.ResourceRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service("bookingMaintenanceService")
@RequiredArgsConstructor
public class MaintenanceService {

    private final ResourceMaintenanceRepository maintenanceRepo;
    private final ResourceRepository resourceRepo;

    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getMaintenanceRecords(Long communityId, Long resourceId) {
        if (resourceId != null) {
            return maintenanceRepo.findByResourceIdAndStatusInOrderByStartDateAsc(
                    resourceId, List.of(MaintenanceStatus.PLANNED, MaintenanceStatus.IN_PROGRESS))
                    .stream().map(this::toResponse).toList();
        }
        return maintenanceRepo.findByCommunityIdOrderByStartDateDesc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public MaintenanceResponse createMaintenance(MaintenanceRequest req, AppUser user, Community community) {
        Resource resource = resourceRepo.findById(req.getResourceId())
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + req.getResourceId()));

        LocalDateTime startDate = LocalDateTime.parse(req.getStartDate());
        LocalDateTime endDate = LocalDateTime.parse(req.getEndDate());

        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        List<ResourceMaintenance> overlapping = maintenanceRepo.findOverlapping(
                resource.getId(), startDate, endDate);
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("Maintenance window overlaps with existing schedule");
        }

        ResourceMaintenance maintenance = ResourceMaintenance.builder()
                .resource(resource)
                .startDate(startDate)
                .endDate(endDate)
                .reason(req.getReason())
                .maintenanceType(MaintenanceType.valueOf(req.getMaintenanceType()))
                .status(MaintenanceStatus.PLANNED)
                .cost(req.getCost() != null ? BigDecimal.valueOf(req.getCost()) : null)
                .vendorName(req.getVendorName())
                .vendorContact(req.getVendorContact())
                .notes(req.getNotes())
                .isRecurring(req.getIsRecurring() != null ? req.getIsRecurring() : false)
                .recurringPattern(req.getRecurringPattern())
                .community(community)
                .createdBy(user)
                .build();

        return toResponse(maintenanceRepo.save(maintenance));
    }

    @Transactional
    public MaintenanceResponse updateMaintenance(Long id, MaintenanceRequest req) {
        ResourceMaintenance maintenance = maintenanceRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance record not found: " + id));

        if (req.getStartDate() != null) maintenance.setStartDate(LocalDateTime.parse(req.getStartDate()));
        if (req.getEndDate() != null) maintenance.setEndDate(LocalDateTime.parse(req.getEndDate()));
        if (req.getReason() != null) maintenance.setReason(req.getReason());
        if (req.getMaintenanceType() != null) {
            maintenance.setMaintenanceType(MaintenanceType.valueOf(req.getMaintenanceType()));
        }
        if (req.getCost() != null) maintenance.setCost(BigDecimal.valueOf(req.getCost()));
        if (req.getVendorName() != null) maintenance.setVendorName(req.getVendorName());
        if (req.getVendorContact() != null) maintenance.setVendorContact(req.getVendorContact());
        if (req.getNotes() != null) maintenance.setNotes(req.getNotes());

        return toResponse(maintenanceRepo.save(maintenance));
    }

    private MaintenanceResponse toResponse(ResourceMaintenance m) {
        return MaintenanceResponse.builder()
                .id(m.getId())
                .resourceId(m.getResource().getId())
                .resourceName(m.getResource().getName())
                .startDate(m.getStartDate().toString())
                .endDate(m.getEndDate().toString())
                .reason(m.getReason())
                .maintenanceType(m.getMaintenanceType())
                .status(m.getStatus())
                .cost(m.getCost())
                .vendorName(m.getVendorName())
                .vendorContact(m.getVendorContact())
                .notes(m.getNotes())
                .isRecurring(m.isRecurring())
                .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : null)
                .build();
    }
}
