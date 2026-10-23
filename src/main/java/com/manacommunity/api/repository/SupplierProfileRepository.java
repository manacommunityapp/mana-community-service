package com.manacommunity.api.repository;

import com.manacommunity.api.model.ProcurementCategory;
import com.manacommunity.api.model.SupplierProfile;
import com.manacommunity.api.model.SupplierVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierProfileRepository extends JpaRepository<SupplierProfile, Long> {

    List<SupplierProfile> findByActiveTrue();

    List<SupplierProfile> findByActiveTrueAndVerificationStatus(SupplierVerificationStatus status);

    @Query("SELECT s FROM SupplierProfile s WHERE s.active = true " +
           "AND (:city IS NULL OR LOWER(s.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "AND (:search IS NULL OR LOWER(s.businessName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(s.categories) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<SupplierProfile> search(@Param("city") String city, @Param("search") String search);

    @Query("SELECT COUNT(s) FROM SupplierProfile s WHERE s.active = true AND s.verificationStatus = 'VERIFIED'")
    long countVerifiedSuppliers();
}
