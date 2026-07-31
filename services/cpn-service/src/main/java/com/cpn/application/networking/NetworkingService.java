package com.cpn.application.networking;

import com.cpn.domain.networking.model.Connection;
import com.cpn.domain.networking.model.ConnectionStatus;
import com.cpn.domain.networking.repository.ConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NetworkingService {

    private final ConnectionRepository connectionRepository;

    @Transactional
    public Connection sendConnectionRequest(UUID requesterId, UUID recipientId, String message) {
        Connection conn = connectionRepository.findBetweenUsers(requesterId, recipientId)
                .orElse(Connection.builder()
                        .requesterId(requesterId)
                        .recipientId(recipientId)
                        .status(ConnectionStatus.PENDING)
                        .message(message)
                        .build());
        conn.setStatus(ConnectionStatus.PENDING);
        conn.setMessage(message);
        return connectionRepository.save(conn);
    }

    @Transactional
    public Connection respondToRequest(UUID connectionId, ConnectionStatus status) {
        Connection conn = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found"));
        conn.setStatus(status);
        return connectionRepository.save(conn);
    }

    @Transactional(readOnly = true)
    public List<Connection> getPendingRequests(UUID userId) {
        return connectionRepository.findByRecipientIdAndStatus(userId, ConnectionStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<Connection> getAcceptedConnections(UUID userId) {
        return connectionRepository.findAllAcceptedConnectionsForUser(userId);
    }
}
