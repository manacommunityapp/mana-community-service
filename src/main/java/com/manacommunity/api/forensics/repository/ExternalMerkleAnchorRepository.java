package com.manacommunity.api.forensics.repository;

import com.manacommunity.api.forensics.entity.ExternalMerkleAnchorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalMerkleAnchorRepository extends JpaRepository<ExternalMerkleAnchorEntity, UUID> {

    Optional<ExternalMerkleAnchorEntity> findTopBySocietyIdOrderBySeqToDesc(Long societyId);

    List<ExternalMerkleAnchorEntity> findBySocietyIdOrderBySeqToDesc(Long societyId);
}
