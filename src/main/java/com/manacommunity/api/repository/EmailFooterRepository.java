package com.manacommunity.api.repository;

import com.manacommunity.api.model.EmailFooter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailFooterRepository extends JpaRepository<EmailFooter, Long> {

    List<EmailFooter> findByCommunity_IdOrderByNameAscIdAsc(Long communityId);

    Optional<EmailFooter> findByIdAndCommunity_Id(Long id, Long communityId);

    Optional<EmailFooter> findFirstByCommunity_IdAndActiveTrueOrderByUpdatedAtDesc(Long communityId);

    Optional<EmailFooter> findByCommunity_IdAndName(Long communityId, String name);
}
