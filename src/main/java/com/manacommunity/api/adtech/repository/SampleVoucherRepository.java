package com.manacommunity.api.adtech.repository;

import com.manacommunity.api.adtech.entity.SampleVoucherEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SampleVoucherRepository extends JpaRepository<SampleVoucherEntity, String> {

    List<SampleVoucherEntity> findBySocietyIdAndDeletedFalseOrderByValidUntilAsc(String societyId);

    Optional<SampleVoucherEntity> findByIdAndDeletedFalse(String id);
}
