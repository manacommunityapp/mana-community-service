package com.manacommunity.api.cfbos.treasury.repository;

import com.manacommunity.api.cfbos.treasury.entity.FundAccount;
import com.manacommunity.api.cfbos.treasury.enums.FundType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FundAccountRepository extends JpaRepository<FundAccount, Long> {
    List<FundAccount> findByCommunityId(Long communityId);
    Optional<FundAccount> findByCommunityIdAndFundType(Long communityId, FundType fundType);
}
