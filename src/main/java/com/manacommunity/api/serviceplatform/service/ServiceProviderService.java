package com.manacommunity.api.serviceplatform.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Vendor;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.serviceplatform.dto.request.CreateOfferingRequest;
import com.manacommunity.api.serviceplatform.dto.request.RegisterProviderRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceOfferingResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceProviderResponse;
import com.manacommunity.api.serviceplatform.entity.ProviderServiceOffering;
import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.enums.PricingUnit;
import com.manacommunity.api.serviceplatform.entity.enums.ProviderType;
import com.manacommunity.api.serviceplatform.entity.enums.VerificationStatus;
import com.manacommunity.api.serviceplatform.repository.ProviderServiceOfferingRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceProviderService {

    private final ServiceProviderRepository providerRepository;
    private final ProviderServiceOfferingRepository offeringRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final AuditService auditService;

    @Transactional
    public ServiceProviderResponse register(RegisterProviderRequest req, AppUser user) {
        if (providerRepository.existsByUserId(user.getId())) {
            throw new IllegalArgumentException("User is already registered as a service provider");
        }

        ServiceProvider provider = ServiceProvider.builder()
                .user(user)
                .community(user.getCommunity())
                .providerType(parseEnum(ProviderType.class, req.getProviderType()))
                .businessName(req.getBusinessName())
                .phone(req.getPhone())
                .email(req.getEmail())
                .bio(req.getBio())
                .profileImageUrl(req.getProfileImageUrl())
                .serviceAreas(req.getServiceAreas())
                .certifications(req.getCertifications())
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        if (req.getVendorId() != null) {
            Vendor vendor = new Vendor();
            vendor.setId(req.getVendorId());
            provider.setVendor(vendor);
        }

        provider = providerRepository.save(provider);
        auditService.record(AuditAction.SERVICE_PROVIDER_REGISTERED, AuditModule.SERVICE_PLATFORM,
                "ServiceProvider", String.valueOf(provider.getId()));
        return toResponse(provider);
    }

    @Transactional(readOnly = true)
    public ServiceProviderResponse getProfile(Long userId) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));
        ServiceProviderResponse resp = toResponse(provider);
        resp.setOfferings(provider.getOfferings().stream()
                .map(this::toOfferingResponse)
                .collect(Collectors.toList()));
        return resp;
    }

    @Transactional
    public ServiceProviderResponse updateProfile(Long userId, RegisterProviderRequest req) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));

        provider.setBusinessName(req.getBusinessName());
        provider.setPhone(req.getPhone());
        provider.setEmail(req.getEmail());
        provider.setBio(req.getBio());
        provider.setProfileImageUrl(req.getProfileImageUrl());
        provider.setServiceAreas(req.getServiceAreas());
        provider.setCertifications(req.getCertifications());

        provider = providerRepository.save(provider);
        return toResponse(provider);
    }

    @Transactional(readOnly = true)
    public Page<ServiceProviderResponse> listProviders(Long communityId, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        Page<ServiceProvider> providers;
        if (status != null && !status.isBlank()) {
            VerificationStatus vs = parseEnum(VerificationStatus.class, status);
            providers = providerRepository.findByCommunityIdAndVerificationStatus(communityId, vs, pageable);
        } else {
            providers = providerRepository.findByCommunityIdAndVerificationStatusNot(
                    communityId, VerificationStatus.SUSPENDED, pageable);
        }
        return providers.map(this::toResponse);
    }

    @Transactional
    public ServiceProviderResponse verifyProvider(Long providerId, String action) {
        ServiceProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", providerId));

        VerificationStatus newStatus = parseEnum(VerificationStatus.class, action);
        provider.setVerificationStatus(newStatus);
        provider = providerRepository.save(provider);

        auditService.record(AuditAction.SERVICE_PROVIDER_VERIFIED, AuditModule.SERVICE_PLATFORM,
                "ServiceProvider", String.valueOf(providerId));
        return toResponse(provider);
    }

    @Transactional
    public ServiceOfferingResponse createOffering(Long userId, CreateOfferingRequest req) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));

        if (provider.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new UnauthorizedActionException("Only verified providers can create offerings");
        }

        ServiceCategory category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", req.getCategoryId()));

        ProviderServiceOffering offering = ProviderServiceOffering.builder()
                .provider(provider)
                .category(category)
                .title(req.getTitle())
                .description(req.getDescription())
                .basePrice(req.getBasePrice())
                .pricingUnit(parseEnum(PricingUnit.class, req.getPricingUnit()))
                .estimatedDurationMinutes(req.getEstimatedDurationMinutes())
                .minOrderValue(req.getMinOrderValue())
                .customFieldValues(req.getCustomFieldValues())
                .tags(req.getTags())
                .available(true)
                .build();

        offering = offeringRepository.save(offering);
        auditService.record(AuditAction.SERVICE_OFFERING_CREATED, AuditModule.SERVICE_PLATFORM,
                "ProviderServiceOffering", String.valueOf(offering.getId()));
        return toOfferingResponse(offering);
    }

    @Transactional
    public ServiceOfferingResponse updateOffering(Long userId, Long offeringId, CreateOfferingRequest req) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));

        ProviderServiceOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("ProviderServiceOffering", offeringId));

        if (!offering.getProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedActionException("Cannot modify another provider's offering");
        }

        offering.setTitle(req.getTitle());
        offering.setDescription(req.getDescription());
        offering.setBasePrice(req.getBasePrice());
        offering.setPricingUnit(parseEnum(PricingUnit.class, req.getPricingUnit()));
        offering.setEstimatedDurationMinutes(req.getEstimatedDurationMinutes());
        offering.setMinOrderValue(req.getMinOrderValue());
        offering.setCustomFieldValues(req.getCustomFieldValues());
        offering.setTags(req.getTags());

        offering = offeringRepository.save(offering);
        auditService.record(AuditAction.SERVICE_OFFERING_UPDATED, AuditModule.SERVICE_PLATFORM,
                "ProviderServiceOffering", String.valueOf(offering.getId()));
        return toOfferingResponse(offering);
    }

    @Transactional
    public void deleteOffering(Long userId, Long offeringId) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));

        ProviderServiceOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("ProviderServiceOffering", offeringId));

        if (!offering.getProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedActionException("Cannot delete another provider's offering");
        }

        offeringRepository.delete(offering);
        auditService.record(AuditAction.SERVICE_OFFERING_DELETED, AuditModule.SERVICE_PLATFORM,
                "ProviderServiceOffering", String.valueOf(offeringId));
    }

    @Transactional(readOnly = true)
    public List<ServiceOfferingResponse> listMyOfferings(Long userId) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));
        return offeringRepository.findByProviderId(provider.getId()).stream()
                .map(this::toOfferingResponse)
                .collect(Collectors.toList());
    }

    private ServiceProviderResponse toResponse(ServiceProvider p) {
        return ServiceProviderResponse.builder()
                .id(p.getId())
                .userId(p.getUser() != null ? p.getUser().getId() : null)
                .userName(p.getUser() != null ? p.getUser().getFullName() : null)
                .vendorId(p.getVendor() != null ? p.getVendor().getId() : null)
                .providerType(p.getProviderType().name())
                .businessName(p.getBusinessName())
                .phone(p.getPhone())
                .email(p.getEmail())
                .bio(p.getBio())
                .profileImageUrl(p.getProfileImageUrl())
                .verificationStatus(p.getVerificationStatus().name())
                .avgRating(p.getAvgRating())
                .totalJobsCompleted(p.getTotalJobsCompleted())
                .serviceAreas(p.getServiceAreas())
                .certifications(p.getCertifications())
                .offerings(Collections.emptyList())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private ServiceOfferingResponse toOfferingResponse(ProviderServiceOffering o) {
        return ServiceOfferingResponse.builder()
                .id(o.getId())
                .providerId(o.getProvider() != null ? o.getProvider().getId() : null)
                .providerName(o.getProvider() != null && o.getProvider().getUser() != null
                        ? o.getProvider().getUser().getFullName() : null)
                .categoryId(o.getCategory() != null ? o.getCategory().getId() : null)
                .categoryName(o.getCategory() != null ? o.getCategory().getName() : null)
                .title(o.getTitle())
                .description(o.getDescription())
                .basePrice(o.getBasePrice())
                .pricingUnit(o.getPricingUnit().name())
                .estimatedDurationMinutes(o.getEstimatedDurationMinutes())
                .minOrderValue(o.getMinOrderValue())
                .available(o.isAvailable())
                .customFieldValues(o.getCustomFieldValues())
                .tags(o.getTags())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
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
