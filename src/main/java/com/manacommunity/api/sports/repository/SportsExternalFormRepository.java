package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsExternalForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SportsExternalFormRepository extends JpaRepository<SportsExternalForm, Long> {

    List<SportsExternalForm> findByEventId(Long eventId);

    List<SportsExternalForm> findByCommunityId(Long communityId);

    Optional<SportsExternalForm> findByWebhookToken(String webhookToken);

    Optional<SportsExternalForm> findByExternalFormId(String externalFormId);

    @Query("SELECT f FROM SportsExternalForm f JOIN FETCH f.fieldMappings WHERE f.id = :id")
    Optional<SportsExternalForm> findByIdWithMappings(@Param("id") Long id);

    @Query("SELECT f FROM SportsExternalForm f JOIN FETCH f.fieldMappings WHERE f.webhookToken = :token")
    Optional<SportsExternalForm> findByWebhookTokenWithMappings(@Param("token") String token);
}
