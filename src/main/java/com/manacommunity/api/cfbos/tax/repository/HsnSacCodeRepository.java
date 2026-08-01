package com.manacommunity.api.cfbos.tax.repository;

import com.manacommunity.api.cfbos.tax.entity.HsnSacCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HsnSacCodeRepository extends JpaRepository<HsnSacCode, Long> {
    Optional<HsnSacCode> findByCode(String code);
}
