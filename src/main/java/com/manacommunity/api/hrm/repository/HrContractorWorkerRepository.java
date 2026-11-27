package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrContractorWorker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HrContractorWorkerRepository extends JpaRepository<HrContractorWorker, Long> {
    List<HrContractorWorker> findByOrganizationId(Long organizationId);
    List<HrContractorWorker> findByAgencyId(Long agencyId);
    Optional<HrContractorWorker> findByWorkerCode(String workerCode);
    Optional<HrContractorWorker> findByFaceEmbeddingHash(String hash);
}
