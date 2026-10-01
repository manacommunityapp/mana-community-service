package com.manacommunity.api.cfbos.payment.repository;
import com.manacommunity.api.cfbos.payment.entity.CfbosPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface CfbosPaymentRepository extends JpaRepository<CfbosPayment, Long> { Optional<CfbosPayment> findByPaymentNumber(String paymentNumber); List<CfbosPayment> findByResidentId(Long residentId); List<CfbosPayment> findByPropertyId(Long propertyId); }
