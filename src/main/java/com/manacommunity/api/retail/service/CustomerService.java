package com.manacommunity.api.retail.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.retail.dto.CustomerDto;
import com.manacommunity.api.retail.entity.Customer;
import com.manacommunity.api.retail.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;

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
        return toDto(repository.save(entity));
    }

    @Transactional
    public CustomerDto updateCustomer(Long id, CustomerDto dto) {
        Customer entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        entity.setName(dto.getName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        return toDto(repository.save(entity));
    }

    @Transactional
    public void deleteCustomer(Long id) {
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
