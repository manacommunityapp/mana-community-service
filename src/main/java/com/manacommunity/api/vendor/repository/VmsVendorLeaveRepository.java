package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsVendorLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface VmsVendorLeaveRepository extends JpaRepository<VmsVendorLeave, Long> {
    List<VmsVendorLeave> findByVendorIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long vendorId, String status, LocalDate start, LocalDate end);
    List<VmsVendorLeave> findByVendorIdAndReason(Long vendorId, String reason);
}
