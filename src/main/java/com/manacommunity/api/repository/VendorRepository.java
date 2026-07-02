package com.manacommunity.api.repository;

import com.manacommunity.api.model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    List<Vendor> findByActiveTrueOrderByNameAsc();
}
