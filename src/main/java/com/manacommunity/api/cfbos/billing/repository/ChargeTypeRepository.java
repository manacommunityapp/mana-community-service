package com.manacommunity.api.cfbos.billing.repository;
import com.manacommunity.api.cfbos.billing.entity.ChargeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface ChargeTypeRepository extends JpaRepository<ChargeType, Long> { Optional<ChargeType> findByCode(String code); List<ChargeType> findByChargeHeadId(Long chargeHeadId); }
