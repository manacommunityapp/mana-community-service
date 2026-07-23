package com.manacommunity.api.serviceplatform.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.serviceplatform.dto.request.CreateServiceCategoryRequest;
import com.manacommunity.api.serviceplatform.dto.request.CreateServiceDomainRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceCategoryResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceDomainResponse;
import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.ServiceDomain;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceDomainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceCatalogService {

    private final ServiceDomainRepository domainRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final AuditService auditService;

    @Transactional
    public ServiceDomainResponse createDomain(CreateServiceDomainRequest req, Long communityId) {
        if (domainRepository.existsBySlugAndCommunityId(req.getSlug(), communityId)) {
            throw new IllegalArgumentException("Domain slug already exists: " + req.getSlug());
        }

        Community community = new Community();
        community.setId(communityId);

        ServiceDomain domain = ServiceDomain.builder()
                .community(community)
                .name(req.getName())
                .slug(req.getSlug())
                .icon(req.getIcon())
                .description(req.getDescription())
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .metadata(req.getMetadata())
                .active(true)
                .build();

        domain = domainRepository.save(domain);
        auditService.record(AuditAction.SERVICE_DOMAIN_CREATED, AuditModule.SERVICE_PLATFORM,
                "ServiceDomain", String.valueOf(domain.getId()));
        return toResponse(domain);
    }

    @Transactional
    public ServiceDomainResponse updateDomain(Long id, CreateServiceDomainRequest req) {
        ServiceDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceDomain", id));

        domain.setName(req.getName());
        domain.setSlug(req.getSlug());
        domain.setIcon(req.getIcon());
        domain.setDescription(req.getDescription());
        if (req.getDisplayOrder() != null) {
            domain.setDisplayOrder(req.getDisplayOrder());
        }
        domain.setMetadata(req.getMetadata());

        domain = domainRepository.save(domain);
        auditService.record(AuditAction.SERVICE_DOMAIN_UPDATED, AuditModule.SERVICE_PLATFORM,
                "ServiceDomain", String.valueOf(domain.getId()));
        return toResponse(domain);
    }

    @Transactional
    public void deleteDomain(Long id) {
        ServiceDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceDomain", id));
        domain.setActive(false);
        domainRepository.save(domain);
        auditService.record(AuditAction.SERVICE_DOMAIN_DELETED, AuditModule.SERVICE_PLATFORM,
                "ServiceDomain", String.valueOf(id));
    }

    @Transactional(readOnly = true)
    public List<ServiceDomainResponse> listDomains(Long communityId) {
        List<ServiceDomain> domains;
        if (communityId != null) {
            domains = domainRepository.findByCommunityIdAndActiveTrueOrderByDisplayOrderAsc(communityId);
        } else {
            domains = domainRepository.findByCommunityIdIsNullAndActiveTrueOrderByDisplayOrderAsc();
        }
        return domains.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceDomainResponse getDomain(Long id) {
        ServiceDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceDomain", id));
        ServiceDomainResponse resp = toResponse(domain);
        resp.setCategories(domain.getCategories().stream()
                .filter(ServiceCategory::isActive)
                .map(this::toCategoryResponse)
                .collect(Collectors.toList()));
        return resp;
    }

    @Transactional
    public ServiceCategoryResponse createCategory(CreateServiceCategoryRequest req) {
        ServiceDomain domain = domainRepository.findById(req.getDomainId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceDomain", req.getDomainId()));

        if (categoryRepository.existsBySlugAndDomainId(req.getSlug(), req.getDomainId())) {
            throw new IllegalArgumentException("Category slug already exists in this domain: " + req.getSlug());
        }

        ServiceCategory parent = null;
        if (req.getParentCategoryId() != null) {
            parent = categoryRepository.findById(req.getParentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", req.getParentCategoryId()));
        }

        ServiceCategory category = ServiceCategory.builder()
                .domain(domain)
                .parentCategory(parent)
                .name(req.getName())
                .slug(req.getSlug())
                .icon(req.getIcon())
                .description(req.getDescription())
                .requiredCertifications(req.getRequiredCertifications())
                .customFields(req.getCustomFields())
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .active(true)
                .build();

        category = categoryRepository.save(category);
        auditService.record(AuditAction.SERVICE_CATEGORY_CREATED, AuditModule.SERVICE_PLATFORM,
                "ServiceCategory", String.valueOf(category.getId()));
        return toCategoryResponse(category);
    }

    @Transactional
    public ServiceCategoryResponse updateCategory(Long id, CreateServiceCategoryRequest req) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", id));

        category.setName(req.getName());
        category.setSlug(req.getSlug());
        category.setIcon(req.getIcon());
        category.setDescription(req.getDescription());
        category.setRequiredCertifications(req.getRequiredCertifications());
        category.setCustomFields(req.getCustomFields());
        if (req.getDisplayOrder() != null) {
            category.setDisplayOrder(req.getDisplayOrder());
        }

        category = categoryRepository.save(category);
        auditService.record(AuditAction.SERVICE_CATEGORY_UPDATED, AuditModule.SERVICE_PLATFORM,
                "ServiceCategory", String.valueOf(category.getId()));
        return toCategoryResponse(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", id));
        category.setActive(false);
        categoryRepository.save(category);
        auditService.record(AuditAction.SERVICE_CATEGORY_DELETED, AuditModule.SERVICE_PLATFORM,
                "ServiceCategory", String.valueOf(id));
    }

    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> listCategories(Long domainId) {
        return categoryRepository.findByDomainIdAndParentCategoryIsNullAndActiveTrueOrderByDisplayOrderAsc(domainId)
                .stream()
                .map(this::toCategoryResponseWithChildren)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceCategoryResponse getCategory(Long id) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", id));
        return toCategoryResponseWithChildren(category);
    }

    private ServiceDomainResponse toResponse(ServiceDomain domain) {
        return ServiceDomainResponse.builder()
                .id(domain.getId())
                .name(domain.getName())
                .slug(domain.getSlug())
                .icon(domain.getIcon())
                .description(domain.getDescription())
                .displayOrder(domain.getDisplayOrder())
                .active(domain.isActive())
                .metadata(domain.getMetadata())
                .categoryCount(domain.getCategories() != null ? (int) domain.getCategories().stream().filter(ServiceCategory::isActive).count() : 0)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private ServiceCategoryResponse toCategoryResponse(ServiceCategory cat) {
        return ServiceCategoryResponse.builder()
                .id(cat.getId())
                .domainId(cat.getDomain() != null ? cat.getDomain().getId() : null)
                .domainName(cat.getDomain() != null ? cat.getDomain().getName() : null)
                .parentCategoryId(cat.getParentCategory() != null ? cat.getParentCategory().getId() : null)
                .parentCategoryName(cat.getParentCategory() != null ? cat.getParentCategory().getName() : null)
                .name(cat.getName())
                .slug(cat.getSlug())
                .icon(cat.getIcon())
                .description(cat.getDescription())
                .requiredCertifications(cat.getRequiredCertifications())
                .customFields(cat.getCustomFields())
                .displayOrder(cat.getDisplayOrder())
                .active(cat.isActive())
                .createdAt(cat.getCreatedAt())
                .updatedAt(cat.getUpdatedAt())
                .build();
    }

    private ServiceCategoryResponse toCategoryResponseWithChildren(ServiceCategory cat) {
        ServiceCategoryResponse resp = toCategoryResponse(cat);
        if (cat.getSubCategories() != null && !cat.getSubCategories().isEmpty()) {
            resp.setSubCategories(cat.getSubCategories().stream()
                    .filter(ServiceCategory::isActive)
                    .map(this::toCategoryResponseWithChildren)
                    .collect(Collectors.toList()));
        } else {
            resp.setSubCategories(Collections.emptyList());
        }
        return resp;
    }
}
