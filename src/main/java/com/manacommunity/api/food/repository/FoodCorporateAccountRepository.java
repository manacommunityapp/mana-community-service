package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCorporateAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodCorporateAccountRepository extends JpaRepository<FoodCorporateAccount, Long> {

    List<FoodCorporateAccount> findByCommunityId(Long communityId);

    Optional<FoodCorporateAccount> findByIdAndCommunityId(Long id, Long communityId);
}
