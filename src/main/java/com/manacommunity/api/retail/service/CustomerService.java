package com.manacommunity.api.retail.service;

import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.retail.dto.CustomerDto;
import com.manacommunity.api.retail.entity.Customer;
import com.manacommunity.api.retail.repository.CustomerRepository;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CustomerDto> getAllCustomers(Long communityId) {
        return repository.findByCommunityIdOrderByNameAsc(communityId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public CustomerDto createCustomer(CustomerDto dto, Community community) {
        Customer entity = Customer.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .community(community)
                .build();
        Customer saved = repository.save(entity);
        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.RETAIL,
                "Customer", String.valueOf(saved.getId()));
        return toDto(saved);
    }

    @Transactional
    public CustomerDto updateCustomer(Long id, CustomerDto dto, Long callerCommunityId) {
        Customer entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        // IDOR protection: caller must belong to the same community as the customer
        if (entity.getCommunity() == null || !entity.getCommunity().getId().equals(callerCommunityId)) {
            throw new UnauthorizedActionException("Customer does not belong to your community");
        }
        entity.setName(dto.getName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        Customer saved = repository.save(entity);
        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.RETAIL,
                "Customer", String.valueOf(id));
        return toDto(saved);
    }

    @Transactional
    public void deleteCustomer(Long id, Long callerCommunityId) {
        Customer entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        // IDOR protection: caller must belong to the same community as the customer
        if (entity.getCommunity() == null || !entity.getCommunity().getId().equals(callerCommunityId)) {
            throw new UnauthorizedActionException("Customer does not belong to your community");
        }
        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.RETAIL,
                "Customer", String.valueOf(id));
        repository.deleteById(id);
    }

    public String getCustomerName(Long id) {
        return repository.findById(id).map(Customer::getName).orElse("Unknown");
    }

    private CustomerDto toDto(Customer c) {
        return CustomerDto.builder()
                .id(c.getId())
                .name(c.getName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .build();
    }
}
