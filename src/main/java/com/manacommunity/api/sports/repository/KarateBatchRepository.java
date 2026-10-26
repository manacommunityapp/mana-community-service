package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.KarateBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KarateBatchRepository extends JpaRepository<KarateBatch, Long> {
    List<KarateBatch> findByProgramIdOrderByStartDateAsc(Long programId);
    List<KarateBatch> findByProgramIdAndStatusOrderByStartDateAsc(Long programId, String status);
}
