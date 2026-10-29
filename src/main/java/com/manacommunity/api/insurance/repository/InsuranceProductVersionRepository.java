package com.manacommunity.api.insurance.repository;

import com.manacommunity.api.insurance.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface InsuranceProductVersionRepository extends JpaRepository<InsuranceProductVersion, Long> {
    List<InsuranceProductVersion> findByProductIdAndVerificationStatusOrderByVersionNoDesc(Long productId, InsuranceEnums.VerificationStatus status);
    @Query("select coalesce(max(v.versionNo),0) from InsuranceProductVersion v where v.productId = :pid")
    int maxVersion(@Param("pid") Long productId);
}

