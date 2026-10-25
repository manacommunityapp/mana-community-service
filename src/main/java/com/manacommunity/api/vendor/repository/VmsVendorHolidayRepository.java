package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsVendorHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VmsVendorHolidayRepository extends JpaRepository<VmsVendorHoliday, Long> {
    List<VmsVendorHoliday> findByVendorIdOrderByHolidayDate(Long vendorId);
    Optional<VmsVendorHoliday> findByIdAndVendorId(Long id, Long vendorId);
}
