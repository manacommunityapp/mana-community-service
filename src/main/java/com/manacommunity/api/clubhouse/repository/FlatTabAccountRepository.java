package com.manacommunity.api.clubhouse.repository;

import com.manacommunity.api.clubhouse.entity.FlatTabAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlatTabAccountRepository extends JpaRepository<FlatTabAccountEntity, UUID> {
    Optional<FlatTabAccountEntity> findBySocietyIdAndFlatId(Long societyId, Long flatId);
    List<FlatTabAccountEntity> findBySocietyIdOrderByUnitCodeAsc(Long societyId);
}
