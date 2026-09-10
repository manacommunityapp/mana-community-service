package com.manacommunity.api.retail.service;

import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.retail.dto.SupplierDto;
import com.manacommunity.api.retail.entity.Supplier;
import com.manacommunity.api.retail.repository.SupplierRepository;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository repository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<SupplierDto> getAllSuppliers(Long communityId) {
        return repository.findByCommunityIdOrderByNameAsc(communityId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public SupplierDto createSupplier(SupplierDto dto, Community community) {
        Supplier entity = Supplier.builder()
                .name(dto.getName())
                .contactPerson(dto.getContactPerson())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .community(community)
                .build();
        Supplier saved = repository.save(entity);
        auditService.record(AuditAction.RETAIL_SUPPLIER_CREATED, AuditModule.RETAIL,
                "Supplier", String.valueOf(saved.getId()));
        return toDto(saved);
    }

    @Transactional
    public SupplierDto updateSupplier(Long id, SupplierDto dto, Long callerCommunityId) {
        Supplier entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        // IDOR protection: caller must belong to the same community as the supplier
        if (entity.getCommunity() == null || !entity.getCommunity().getId().equals(callerCommunityId)) {
            throw new UnauthorizedActionException("Supplier does not belong to your community");
        }
        entity.setName(dto.getName());
        entity.setContactPerson(dto.getContactPerson());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        Supplier saved = repository.save(entity);
        auditService.record(AuditAction.RETAIL_SUPPLIER_UPDATED, AuditModule.RETAIL,
                "Supplier", String.valueOf(id));
        return toDto(saved);
    }

    @Transactional
    public void deleteSupplier(Long id, Long callerCommunityId) {
        Supplier entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        // IDOR protection: caller must belong to the same community as the supplier
        if (entity.getCommunity() == null || !entity.getCommunity().getId().equals(callerCommunityId)) {
            throw new UnauthorizedActionException("Supplier does not belong to your community");
        }
        auditService.record(AuditAction.RETAIL_SUPPLIER_DELETED, AuditModule.RETAIL,
                "Supplier", String.valueOf(id));
        repository.deleteById(id);
    }

    public String getSupplierName(Long id) {
        return repository.findById(id).map(Supplier::getName).orElse("Unknown");
    }

    private SupplierDto toDto(Supplier s) {
        return SupplierDto.builder()
                .id(s.getId())
                .name(s.getName())
                .contactPerson(s.getContactPerson())
                .phone(s.getPhone())
                .email(s.getEmail())
                .build();
    }
}
