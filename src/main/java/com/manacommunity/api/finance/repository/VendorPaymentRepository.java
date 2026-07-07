package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.VendorPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorPaymentRepository extends JpaRepository<VendorPayment, Long> {
    List<VendorPayment> findAllByOrderByPaymentDateDesc();
    List<VendorPayment> findByPaymentTypeOrderByPaymentDateDesc(VendorPayment.PaymentType paymentType);
}
