package com.manacommunity.api.retail.service;

import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.retail.dto.RetailProductDto;
import com.manacommunity.api.retail.entity.RetailProduct;
import com.manacommunity.api.retail.repository.RetailProductRepository;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RetailProductService {

    private final RetailProductRepository repository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<RetailProductDto> getAllProducts(Long communityId) {
        return repository.findByCommunityIdOrderByNameAsc(communityId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public RetailProductDto createProduct(RetailProductDto dto, Community community) {
        RetailProduct entity = RetailProduct.builder()
                .name(dto.getName())
                .emoji(dto.getEmoji())
                .category(dto.getCategory())
                .unitPrice(dto.getUnitPrice())
                .reorderLevel(dto.getReorderLevel())
                .unitsOrdered(dto.getUnitsOrdered() != null ? dto.getUnitsOrdered() : 0)
                .unitsSold(dto.getUnitsSold() != null ? dto.getUnitsSold() : 0)
                .community(community)
                .build();
        RetailProduct saved = repository.save(entity);
        auditService.record(AuditAction.RETAIL_PRODUCT_CREATED, AuditModule.RETAIL,
                "RetailProduct", String.valueOf(saved.getId()));
        return toDto(saved);
    }

    @Transactional
    public RetailProductDto updateProduct(Long id, RetailProductDto dto, Long callerCommunityId) {
        RetailProduct entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        // IDOR protection: caller must belong to the same community as the product
        if (entity.getCommunity() == null || !entity.getCommunity().getId().equals(callerCommunityId)) {
            throw new UnauthorizedActionException("Product does not belong to your community");
        }
        entity.setName(dto.getName());
        entity.setEmoji(dto.getEmoji());
        entity.setCategory(dto.getCategory());
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setReorderLevel(dto.getReorderLevel());
        if (dto.getUnitsOrdered() != null) entity.setUnitsOrdered(dto.getUnitsOrdered());
        if (dto.getUnitsSold() != null) entity.setUnitsSold(dto.getUnitsSold());
        RetailProduct saved = repository.save(entity);
        auditService.record(AuditAction.RETAIL_PRODUCT_UPDATED, AuditModule.RETAIL,
                "RetailProduct", String.valueOf(id));
        return toDto(saved);
    }

    @Transactional
    public void deleteProduct(Long id, Long callerCommunityId) {
        RetailProduct entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        // IDOR protection: caller must belong to the same community as the product
        if (entity.getCommunity() == null || !entity.getCommunity().getId().equals(callerCommunityId)) {
            throw new UnauthorizedActionException("Product does not belong to your community");
        }
        auditService.record(AuditAction.RETAIL_PRODUCT_DELETED, AuditModule.RETAIL,
                "RetailProduct", String.valueOf(id));
        repository.deleteById(id);
    }

    private RetailProductDto toDto(RetailProduct p) {
        return RetailProductDto.builder()
                .id(p.getId())
                .name(p.getName())
                .emoji(p.getEmoji())
                .category(p.getCategory())
                .unitPrice(p.getUnitPrice())
                .reorderLevel(p.getReorderLevel())
                .unitsOrdered(p.getUnitsOrdered())
                .unitsSold(p.getUnitsSold())
                .build();
    }
}
