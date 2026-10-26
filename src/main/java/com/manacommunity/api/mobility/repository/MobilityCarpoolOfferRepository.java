package com.manacommunity.api.mobility.repository;

import com.manacommunity.api.mobility.model.MobilityCarpoolOffer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MobilityCarpoolOfferRepository extends JpaRepository<MobilityCarpoolOffer, Long> {

    @Query("SELECT o FROM MobilityCarpoolOffer o WHERE o.community.id = :communityId AND o.status = 'ACTIVE'" +
           " AND (:dest IS NULL OR LOWER(o.destination) LIKE LOWER(CONCAT('%', :dest, '%')))" +
           " ORDER BY o.departureTime ASC")
    Page<MobilityCarpoolOffer> findActive(Long communityId, String dest, Pageable pageable);
}
