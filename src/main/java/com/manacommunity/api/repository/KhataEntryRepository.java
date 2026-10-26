package com.manacommunity.api.repository;

import com.manacommunity.api.model.KhataEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KhataEntryRepository extends JpaRepository<KhataEntry, Long> {

    List<KhataEntry> findByPartyIdOrderByEntryDateDescCreatedAtDesc(Long partyId);
}
