package com.manacommunity.api.forensics.repository;

import com.manacommunity.api.forensics.entity.WormAuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WormAuditEventRepository extends JpaRepository<WormAuditEventEntity, UUID> {

    Optional<WormAuditEventEntity> findTopBySocietyIdOrderBySeqNoDesc(Long societyId);

    Optional<WormAuditEventEntity> findBySocietyIdAndSeqNo(Long societyId, Long seqNo);

    List<WormAuditEventEntity> findBySocietyIdOrderBySeqNoAsc(Long societyId);
}
