package com.manacommunity.api.parcel.repository;

import com.manacommunity.api.parcel.entity.ParcelRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParcelRecordRepository extends JpaRepository<ParcelRecord, Long> {
    List<ParcelRecord> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<ParcelRecord> findByCommunityIdAndFlatNoAndStatusOrderByCreatedAtDesc(Long communityId, String flatNo, String status);
    List<ParcelRecord> findByCommunityIdAndFlatNoOrderByCreatedAtDesc(Long communityId, String flatNo);
}
