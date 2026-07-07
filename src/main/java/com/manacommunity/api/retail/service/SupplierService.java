package com.manacommunity.api.retail.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.retail.dto.SupplierDto;
import com.manacommunity.api.retail.entity.Supplier;
import com.manacommunity.api.retail.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository repository;

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
        return toDto(repository.save(entity));
    }

    @Transactional
    public SupplierDto updateSupplier(Long id, SupplierDto dto) {
        Supplier entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        entity.setName(dto.getName());
        entity.setContactPerson(dto.getContactPerson());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        return toDto(repository.save(entity));
    }

    @Transactional
    public void deleteSupplier(Long id) {
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
