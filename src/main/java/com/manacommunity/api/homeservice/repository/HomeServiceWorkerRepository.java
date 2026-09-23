package com.manacommunity.api.homeservice.repository;

import com.manacommunity.api.homeservice.model.entity.HomeServiceWorkerEntity;
import com.manacommunity.api.homeservice.model.enums.HomeServiceVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository("homeServiceWorkerRepository")
public interface HomeServiceWorkerRepository extends JpaRepository<HomeServiceWorkerEntity, String> {
    List<HomeServiceWorkerEntity> findByCommunityIdAndActiveTrue(String communityId);
    List<HomeServiceWorkerEntity> findByVerificationStatus(HomeServiceVerificationStatus status);

    @Query("SELECT w FROM HomeServiceWorkerEntity w WHERE w.communityId = :communityId " +
           "AND w.active = true " +
           "AND (:minRating IS NULL OR w.rating >= :minRating) " +
           "AND (:verificationStatus IS NULL OR w.verificationStatus = :verificationStatus)")
    List<HomeServiceWorkerEntity> searchWorkers(
            @Param("communityId") String communityId,
            @Param("minRating") BigDecimal minRating,
            @Param("verificationStatus") HomeServiceVerificationStatus verificationStatus
    );
}
