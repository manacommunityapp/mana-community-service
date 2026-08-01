package com.manacommunity.api.cfbos.accounting.repository;

import com.manacommunity.api.cfbos.accounting.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByCode(String code);
    List<Account> findByParentAccountIsNullAndIsActiveTrue();
    List<Account> findByParentAccountAndIsActiveTrue(Account parent);
    List<Account> findByIsActiveTrue();
}
