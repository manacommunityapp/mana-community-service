package com.manacommunity.api.repository;

import com.manacommunity.api.model.GeneratorAmcContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratorAmcContractRepository extends JpaRepository<GeneratorAmcContract, Long> {

    List<GeneratorAmcContract> findByGeneratorIdOrderByStartDateDesc(Long generatorId);

    List<GeneratorAmcContract> findByGeneratorCommunityIdOrderByStartDateDesc(Long communityId);
}
