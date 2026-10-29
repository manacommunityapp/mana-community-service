package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceLoyaltyAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommerceLoyaltyAccountRepository extends JpaRepository<CommerceLoyaltyAccount, Long> {

    Optional<CommerceLoyaltyAccount> findByUser_Id(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM CommerceLoyaltyAccount a WHERE a.user.id = :userId")
    Optional<CommerceLoyaltyAccount> findByUserIdForUpdate(@Param("userId") Long userId);
}
