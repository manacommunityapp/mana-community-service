package com.manacommunity.api.delivery.repository;

import com.manacommunity.api.delivery.model.ExpressDeliveryManifest;
import com.manacommunity.api.delivery.model.ExpressPassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpressDeliveryManifestRepository extends JpaRepository<ExpressDeliveryManifest, Long> {
    Optional<ExpressDeliveryManifest> findByManifestCode(String manifestCode);
    List<ExpressDeliveryManifest> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, ExpressPassStatus status);
}
