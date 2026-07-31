package com.cpn.domain.networking.repository;

import com.cpn.domain.networking.model.Connection;
import com.cpn.domain.networking.model.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectionRepository extends JpaRepository<Connection, UUID> {

    @Query("SELECT c FROM Connection c WHERE (c.requesterId = :u1 AND c.recipientId = :u2) OR (c.requesterId = :u2 AND c.recipientId = :u1)")
    Optional<Connection> findBetweenUsers(@Param("u1") UUID u1, @Param("u2") UUID u2);

    List<Connection> findByRecipientIdAndStatus(UUID recipientId, ConnectionStatus status);

    List<Connection> findByRequesterIdAndStatus(UUID requesterId, ConnectionStatus status);

    @Query("SELECT c FROM Connection c WHERE (c.requesterId = :userId OR c.recipientId = :userId) AND c.status = 'ACCEPTED'")
    List<Connection> findAllAcceptedConnectionsForUser(@Param("userId") UUID userId);
}
