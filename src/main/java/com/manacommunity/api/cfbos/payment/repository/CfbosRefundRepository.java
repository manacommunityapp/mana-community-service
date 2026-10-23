package com.manacommunity.api.cfbos.payment.repository;
import com.manacommunity.api.cfbos.payment.entity.CfbosRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface CfbosRefundRepository extends JpaRepository<CfbosRefund, Long> { Optional<CfbosRefund> findByRefundNumber(String refundNumber); List<CfbosRefund> findByPaymentId(Long paymentId); }
