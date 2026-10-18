package com.manacommunity.api.mobility.repository;

import com.manacommunity.api.mobility.model.MobilityDriver;
import com.manacommunity.api.mobility.model.MobilityDriverStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MobilityDriverRepository extends JpaRepository<MobilityDriver, Long> {

    Optional<MobilityDriver> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("SELECT d FROM MobilityDriver d WHERE d.availability = 'ONLINE' AND d.status = 'APPROVED'")
    List<MobilityDriver> findOnlineApprovedDrivers();

    Page<MobilityDriver> findByStatus(MobilityDriverStatus status, Pageable pageable);
}
