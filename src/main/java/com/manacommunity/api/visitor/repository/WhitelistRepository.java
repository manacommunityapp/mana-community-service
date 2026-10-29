package com.manacommunity.api.visitor.repository;

import com.manacommunity.api.visitor.entity.WhitelistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WhitelistRepository extends JpaRepository<WhitelistEntry, Long> {
    List<WhitelistEntry> findByResidentId(Long residentId);
}
