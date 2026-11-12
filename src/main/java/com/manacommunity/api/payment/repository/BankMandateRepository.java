package com.manacommunity.api.payment.repository;

import com.manacommunity.api.payment.entity.BankMandate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BankMandateRepository extends JpaRepository<BankMandate, Long> {
    Optional<BankMandate> findByUserIdAndStatus(Long userId, String status);
}
