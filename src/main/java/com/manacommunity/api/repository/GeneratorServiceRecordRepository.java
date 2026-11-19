package com.manacommunity.api.repository;

import com.manacommunity.api.model.GeneratorServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratorServiceRecordRepository extends JpaRepository<GeneratorServiceRecord, Long> {

    List<GeneratorServiceRecord> findByGeneratorIdOrderByServiceDateDesc(Long generatorId);

    List<GeneratorServiceRecord> findByGeneratorCommunityIdOrderByServiceDateDesc(Long communityId);
}
