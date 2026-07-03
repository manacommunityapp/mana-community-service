package com.manacommunity.api.finance.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.finance.entity.LedgerCustomer;
import com.manacommunity.api.finance.repository.LedgerCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerCustomerService {

    private final LedgerCustomerRepository repo;

    public List<LedgerCustomer> getAll() {
        return repo.findAllByOrderByNameAsc();
    }

    @Transactional
    public LedgerCustomer create(LedgerCustomer in) {
        in.setId(null);
        return repo.save(in);
    }

    @Transactional
    public LedgerCustomer update(Long id, LedgerCustomer in) {
        LedgerCustomer c = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LedgerCustomer", id));
        c.setName(in.getName());
        c.setEmail(in.getEmail());
        c.setPhone(in.getPhone());
        c.setGstType(in.getGstType());
        c.setGstin(in.getGstin());
        c.setCurrency(in.getCurrency());
        c.setOpeningBalance(in.getOpeningBalance());
        c.setBalanceType(in.getBalanceType());
        c.setStartsFrom(in.getStartsFrom());
        c.setBillAddr1(in.getBillAddr1());
        c.setBillAddr2(in.getBillAddr2());
        c.setBillCity(in.getBillCity());
        c.setBillState(in.getBillState());
        c.setBillCountry(in.getBillCountry());
        c.setBillZipcode(in.getBillZipcode());
        c.setShipAddr1(in.getShipAddr1());
        c.setShipAddr2(in.getShipAddr2());
        c.setShipCity(in.getShipCity());
        c.setShipState(in.getShipState());
        c.setShipCountry(in.getShipCountry());
        c.setShipZipcode(in.getShipZipcode());
        return repo.save(c);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("LedgerCustomer", id);
        repo.deleteById(id);
    }
}
