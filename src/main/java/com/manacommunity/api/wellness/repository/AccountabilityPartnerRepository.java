package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.AccountabilityPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountabilityPartnerRepository extends JpaRepository<AccountabilityPartner, Long> {
    List<AccountabilityPartner> findByUserIdAndActiveTrue(Long userId);
    List<AccountabilityPartner> findByPartnerUserIdAndActiveTrue(Long partnerUserId);
}
