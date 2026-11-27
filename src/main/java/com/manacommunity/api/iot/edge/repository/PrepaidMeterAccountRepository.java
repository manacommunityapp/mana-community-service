package com.manacommunity.api.iot.edge.repository;

import com.manacommunity.api.iot.edge.entity.PrepaidMeterAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrepaidMeterAccountRepository extends JpaRepository<PrepaidMeterAccount, Long> {

    Optional<PrepaidMeterAccount> findByMeterId(Long meterId);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT p FROM PrepaidMeterAccount p WHERE p.meterId = :meterId")
    Optional<PrepaidMeterAccount> findByMeterIdForUpdate(@Param("meterId") Long meterId);

    List<PrepaidMeterAccount> findByCommunityId(Long communityId);

    @Query("SELECT p FROM PrepaidMeterAccount p WHERE p.active = true AND p.balance <= p.lowBalanceThreshold AND p.lowBalanceAlertSent = false")
    List<PrepaidMeterAccount> findAccountsNeedingLowBalanceAlert();

    @Query("SELECT p FROM PrepaidMeterAccount p WHERE p.active = true AND p.balance <= :zero AND p.isDisconnected = false")
    List<PrepaidMeterAccount> findAccountsToDisconnect(@Param("zero") BigDecimal zero);
}
