package com.manacommunity.api.finance.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.finance.entity.FinanceVendor;
import com.manacommunity.api.finance.repository.FinanceVendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceVendorService {

    private final FinanceVendorRepository repo;

    public List<FinanceVendor> getAll() {
        return repo.findAllByOrderByNameAsc();
    }

    @Transactional
    public FinanceVendor create(FinanceVendor in) {
        in.setId(null);
        if (in.getStatus() == null || in.getStatus().isBlank()) in.setStatus("Active");
        return repo.save(in);
    }

    @Transactional
    public FinanceVendor update(Long id, FinanceVendor in) {
        FinanceVendor v = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinanceVendor", id));
        v.setName(in.getName());
        v.setContactPerson(in.getContactPerson());
        v.setEmail(in.getEmail());
        v.setPhone(in.getPhone());
        v.setGstin(in.getGstin());
        v.setPan(in.getPan());
        v.setAddress(in.getAddress());
        v.setCity(in.getCity());
        v.setState(in.getState());
        v.setPincode(in.getPincode());
        if (in.getStatus() != null && !in.getStatus().isBlank()) v.setStatus(in.getStatus());
        return repo.save(v);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("FinanceVendor", id);
        repo.deleteById(id);
    }
}
