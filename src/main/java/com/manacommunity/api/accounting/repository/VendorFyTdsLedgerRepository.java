package com.manacommunity.api.accounting.repository;

import com.manacommunity.api.accounting.entity.VendorFyTdsLedgerEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VendorFyTdsLedgerRepository extends JpaRepository<VendorFyTdsLedgerEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT v FROM VendorFyTdsLedgerEntity v
         WHERE v.societyId = :societyId
           AND v.fyCode = :fyCode
           AND v.vendorPan = :vendorPan
           AND v.tdsSection = :tdsSection
        """)
    Optional<VendorFyTdsLedgerEntity> findForUpdate(
            @Param("societyId") Long societyId,
            @Param("fyCode") String fyCode,
            @Param("vendorPan") String vendorPan,
            @Param("tdsSection") String tdsSection
    );
}
