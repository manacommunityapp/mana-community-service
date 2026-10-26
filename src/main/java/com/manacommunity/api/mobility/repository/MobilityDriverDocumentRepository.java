package com.manacommunity.api.mobility.repository;

import com.manacommunity.api.mobility.model.MobilityDriverDocument;
import com.manacommunity.api.mobility.model.MobilityDocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MobilityDriverDocumentRepository extends JpaRepository<MobilityDriverDocument, Long> {

    List<MobilityDriverDocument> findByDriverId(Long driverId);

    Page<MobilityDriverDocument> findByStatus(MobilityDocumentStatus status, Pageable pageable);
}
