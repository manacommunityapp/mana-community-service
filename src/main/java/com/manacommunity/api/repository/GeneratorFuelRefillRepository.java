package com.manacommunity.api.repository;

import com.manacommunity.api.model.GeneratorFuelRefill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratorFuelRefillRepository extends JpaRepository<GeneratorFuelRefill, Long> {

    List<GeneratorFuelRefill> findByGeneratorIdOrderByRefillDateDesc(Long generatorId);

    List<GeneratorFuelRefill> findByGeneratorCommunityIdOrderByRefillDateDesc(Long communityId);
}
