package com.manacommunity.api.cfbos.payment.repository;
import com.manacommunity.api.cfbos.payment.entity.CfbosReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface CfbosReceiptRepository extends JpaRepository<CfbosReceipt, Long> { Optional<CfbosReceipt> findByReceiptNumber(String receiptNumber); List<CfbosReceipt> findByResidentId(Long residentId); Optional<CfbosReceipt> findByPaymentId(Long paymentId); }
