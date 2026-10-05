package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceHandoverPass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommerceHandoverPassRepository extends JpaRepository<CommerceHandoverPass, Long> {
    Optional<CommerceHandoverPass> findByPassCode(String passCode);
    Optional<CommerceHandoverPass> findByOrderId(Long orderId);
    Optional<CommerceHandoverPass> findByOtp(String otp);
}