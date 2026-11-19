package com.manacommunity.api.repository;

import com.manacommunity.api.model.GeneratorRunLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratorRunLogRepository extends JpaRepository<GeneratorRunLog, Long> {

    List<GeneratorRunLog> findByGeneratorIdOrderByStartTimeDesc(Long generatorId);

    List<GeneratorRunLog> findByGeneratorCommunityIdOrderByStartTimeDesc(Long communityId);
}
