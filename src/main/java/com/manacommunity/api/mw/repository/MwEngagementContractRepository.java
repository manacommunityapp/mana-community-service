package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwEngagementContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MwEngagementContractRepository extends JpaRepository<MwEngagementContract, Long> {

    Optional<MwEngagementContract> findByApplicationId(Long applicationId);
}
