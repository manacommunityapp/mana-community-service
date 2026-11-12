package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwPoolApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MwPoolApplicationRepository extends JpaRepository<MwPoolApplication, Long> {

    List<MwPoolApplication> findByListingId(Long listingId);

    List<MwPoolApplication> findByManaUserId(Long manaUserId);

    Optional<MwPoolApplication> findByListingIdAndManaUserId(Long listingId, Long manaUserId);

    boolean existsByListingIdAndManaUserId(Long listingId, Long manaUserId);

    int countByListingIdAndStatus(Long listingId, String status);
}
