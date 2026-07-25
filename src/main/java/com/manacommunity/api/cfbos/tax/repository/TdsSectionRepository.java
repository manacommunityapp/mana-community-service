package com.manacommunity.api.cfbos.tax.repository;

import com.manacommunity.api.cfbos.tax.entity.TdsSection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TdsSectionRepository extends JpaRepository<TdsSection, Long> {
    Optional<TdsSection> findBySectionCode(String sectionCode);
}
