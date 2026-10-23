package com.manacommunity.api.finance.personal.repository;

import com.manacommunity.api.finance.personal.entity.PersonalAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalAccountRepository extends JpaRepository<PersonalAccount, String> {
    List<PersonalAccount> findByUserIdAndIsActiveTrueOrderByCreatedAtAsc(Long userId);
    List<PersonalAccount> findByUserIdOrderByCreatedAtAsc(Long userId);
    Optional<PersonalAccount> findByIdAndUserId(String id, Long userId);
}
