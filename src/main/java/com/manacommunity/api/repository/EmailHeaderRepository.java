package com.manacommunity.api.repository;

import com.manacommunity.api.model.EmailHeader;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailHeaderRepository extends JpaRepository<EmailHeader, Long> {

    List<EmailHeader> findByCommunity_IdOrderByNameAscIdAsc(Long communityId);

    Optional<EmailHeader> findByIdAndCommunity_Id(Long id, Long communityId);

    Optional<EmailHeader> findFirstByCommunity_IdAndActiveTrueOrderByUpdatedAtDesc(Long communityId);

    Optional<EmailHeader> findByCommunity_IdAndName(Long communityId, String name);
}
