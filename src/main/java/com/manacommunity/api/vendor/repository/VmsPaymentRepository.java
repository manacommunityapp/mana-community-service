package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.manacommunity.api.vendor.entity.VmsPayment.PaymentProcessStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VmsPaymentRepository extends JpaRepository<VmsPayment, Long> {
    Page<VmsPayment> findByCommunityId(Long communityId, Pageable pageable);
    Page<VmsPayment> findByVendorId(Long vendorId, Pageable pageable);
    List<VmsPayment> findByInvoiceId(Long invoiceId);
    Optional<VmsPayment> findByIdAndCommunityId(Long id, Long communityId);

    @Query("select coalesce(sum(p.netAmount), 0) from VmsPayment p where p.vendor.id = :vendorId and p.status = :status")
    BigDecimal sumNetByVendorAndStatus(@Param("vendorId") Long vendorId, @Param("status") PaymentProcessStatus status);

    @Query("select coalesce(sum(p.netAmount), 0) from VmsPayment p where p.vendor.id = :vendorId and p.status = :status and p.createdAt >= :from")
    BigDecimal sumNetByVendorStatusSince(@Param("vendorId") Long vendorId, @Param("status") PaymentProcessStatus status, @Param("from") LocalDateTime from);

    @Query("select coalesce(sum(p.amount), 0) from VmsPayment p where p.vendor.id = :vendorId and p.status in :statuses")
    BigDecimal sumAmountByVendorAndStatuses(@Param("vendorId") Long vendorId, @Param("statuses") java.util.Collection<PaymentProcessStatus> statuses);
}
