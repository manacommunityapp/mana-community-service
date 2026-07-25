package com.manacommunity.api.cfbos.accounting.repository;

import com.manacommunity.api.cfbos.accounting.entity.AccountGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountGroupRepository extends JpaRepository<AccountGroup, Long> {
    Optional<AccountGroup> findByCode(String code);
}
