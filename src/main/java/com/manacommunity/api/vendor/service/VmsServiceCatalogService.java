package com.manacommunity.api.vendor.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.vendor.dto.*;
import com.manacommunity.api.vendor.entity.*;
import com.manacommunity.api.vendor.repository.VmsVendorCategoryRepository;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import com.manacommunity.api.vendor.repository.VmsVendorServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VmsServiceCatalogService {

    private final VmsVendorServiceRepository serviceRepo;
    private final VmsVendorRepository vendorRepo;
    private final VmsVendorCategoryRepository categoryRepo;

    @Transactional(readOnly = true)
    public Page<VendorServiceResponse> getServices(Long communityId, Long categoryId, Pageable pageable) {
        if (categoryId != null) {
            return serviceRepo.findByCommunityIdAndCategoryIdAndStatus(communityId, categoryId, VmsVendorService.ServiceStatus.ACTIVE, pageable)
                    .map(this::toResponse);
        }
        return serviceRepo.findByCommunityIdAndStatus(communityId, VmsVendorService.ServiceStatus.ACTIVE, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<VendorServiceResponse> searchServices(Long communityId, String query, Pageable pageable) {
        return serviceRepo.searchByCommunity(communityId, query, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<VendorServiceResponse> getVendorServices(Long vendorId) {
        return serviceRepo.findByVendorIdAndStatus(vendorId, VmsVendorService.ServiceStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VendorServiceResponse getById(Long id) {
        return toResponse(serviceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", id)));
    }

    @Transactional
    public VendorServiceResponse create(VendorServiceRequest req, Long vendorId, Community community) {
        VmsVendor vendor = vendorRepo.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", vendorId));
        VmsVendorCategory category = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));

        VmsVendorService svc = VmsVendorService.builder()
                .name(req.getName())
                .description(req.getDescription())
                .category(category)
                .vendor(vendor)
                .basePrice(req.getBasePrice())
                .priceUnit(req.getPriceUnit())
                .durationMinutes(req.getDurationMinutes())
                .taxPercent(req.getTaxPercent())
                .isAvailable(req.getIsAvailable() != null ? req.getIsAvailable() : true)
                .maxBookingsPerDay(req.getMaxBookingsPerDay())
                .community(community)
                .build();

        return toResponse(serviceRepo.save(svc));
    }

    @Transactional
    public VendorServiceResponse update(Long id, VendorServiceRequest req) {
        VmsVendorService svc = serviceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", id));
        svc.setName(req.getName());
        svc.setDescription(req.getDescription());
        svc.setBasePrice(req.getBasePrice());
        svc.setPriceUnit(req.getPriceUnit());
        svc.setDurationMinutes(req.getDurationMinutes());
        svc.setTaxPercent(req.getTaxPercent());
        if (req.getIsAvailable() != null) svc.setIsAvailable(req.getIsAvailable());
        if (req.getMaxBookingsPerDay() != null) svc.setMaxBookingsPerDay(req.getMaxBookingsPerDay());
        return toResponse(serviceRepo.save(svc));
    }

    @Transactional
    public void delete(Long id) {
        VmsVendorService svc = serviceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", id));
        svc.setStatus(VmsVendorService.ServiceStatus.DELETED);
        serviceRepo.save(svc);
    }

    private VendorServiceResponse toResponse(VmsVendorService s) {
        VmsVendor v = s.getVendor();
        return VendorServiceResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .categoryId(s.getCategory() != null ? s.getCategory().getId() : null)
                .categoryName(s.getCategory() != null ? s.getCategory().getName() : null)
                .basePrice(s.getBasePrice())
                .priceUnit(s.getPriceUnit())
                .durationMinutes(s.getDurationMinutes())
                .taxPercent(s.getTaxPercent())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .isAvailable(s.getIsAvailable())
                .maxBookingsPerDay(s.getMaxBookingsPerDay())
                .vendor(VendorServiceResponse.VendorRef.builder()
                        .id(v.getId())
                        .businessName(v.getBusinessName())
                        .logoUrl(v.getLogoUrl())
                        .avgRating(v.getAvgRating())
                        .build())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
