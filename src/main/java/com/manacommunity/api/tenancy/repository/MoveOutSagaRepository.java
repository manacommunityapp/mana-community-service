package com.manacommunity.api.tenancy.repository;

import com.manacommunity.api.tenancy.entity.MoveOutSaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MoveOutSagaRepository extends JpaRepository<MoveOutSaga, Long> {

    Optional<MoveOutSaga> findByTenantRecordId(Long tenantRecordId);

    List<MoveOutSaga> findByOverallStatus(MoveOutSaga.SagaStatus status);

    List<MoveOutSaga> findByOverallStatusIn(List<MoveOutSaga.SagaStatus> statuses);
}
