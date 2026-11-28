package com.manacommunity.api.erpaudit.repository;

import com.manacommunity.api.erpaudit.entity.PaymentWebhookTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentWebhookTestRepository extends JpaRepository<PaymentWebhookTest, Long> {}
