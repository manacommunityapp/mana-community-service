package com.manacommunity.api.cfbos.billing.repository;
import com.manacommunity.api.cfbos.billing.entity.ChargeHead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface ChargeHeadRepository extends JpaRepository<ChargeHead, Long> { Optional<ChargeHead> findByCode(String code); }
