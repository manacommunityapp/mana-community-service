package com.manacommunity.api.repository;

import com.manacommunity.api.model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    List<Vendor> findByActiveTrueOrderByNameAsc();
    long countByActiveTrue();
    long countByActiveTrueAndContractEndDateBefore(LocalDate date);
    long countByActiveTrueAndContractEndDateBetween(LocalDate from, LocalDate to);
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
