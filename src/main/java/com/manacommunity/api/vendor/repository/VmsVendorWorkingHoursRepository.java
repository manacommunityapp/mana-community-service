package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsVendorWorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VmsVendorWorkingHoursRepository extends JpaRepository<VmsVendorWorkingHours, Long> {
    List<VmsVendorWorkingHours> findByVendorIdOrderByDayOfWeek(Long vendorId);
    void deleteByVendorId(Long vendorId);
}
