package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.ChartOfAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long> {

    List<ChartOfAccount> findByCommunityIdOrderByCode(Long communityId);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM ChartOfAccount a WHERE a.communityId = :communityId AND a.type = :type")
    BigDecimal sumBalanceByType(Long communityId, ChartOfAccount.AccountType type);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM ChartOfAccount a WHERE a.communityId = :communityId AND a.isReserveFund = true")
    BigDecimal sumReserveFundBalances(Long communityId);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM ChartOfAccount a WHERE a.communityId = :communityId " +
           "AND a.type = com.manacommunity.api.finance.entity.ChartOfAccount.AccountType.ASSET " +
           "AND (UPPER(a.name) LIKE '%FIXED DEPOSIT%' OR UPPER(a.name) LIKE '% FD %' OR UPPER(a.name) LIKE '%FD')")
    BigDecimal sumFixedDepositBalances(Long communityId);
}
