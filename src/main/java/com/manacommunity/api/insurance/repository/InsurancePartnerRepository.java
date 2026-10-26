package com.manacommunity.api.insurance.repository;

import com.manacommunity.api.insurance.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface InsurancePartnerRepository extends JpaRepository<InsurancePartner, Long> {
    Optional<InsurancePartner> findByCode(String code);
    List<InsurancePartner> findByStatusAndAuthorisationVerifiedTrue(InsuranceEnums.PartnerStatus status);
}

