package com.cpn.application.business;

import com.cpn.domain.business.model.Business;
import com.cpn.domain.business.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;

    @Transactional(readOnly = true)
    public List<Business> getAllBusinesses() {
        return businessRepository.findAll();
    }

    @Transactional
    public Business registerBusiness(Business business) {
        business.setVerified(false);
        return businessRepository.save(business);
    }
}
