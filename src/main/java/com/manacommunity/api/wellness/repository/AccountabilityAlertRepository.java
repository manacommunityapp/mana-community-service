package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.AccountabilityAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountabilityAlertRepository extends JpaRepository<AccountabilityAlert, Long> {
    List<AccountabilityAlert> findByUserIdOrderByTriggeredAtDesc(Long userId);
    List<AccountabilityAlert> findByUserIdAndIsReadFalseOrderByTriggeredAtDesc(Long userId);
}
