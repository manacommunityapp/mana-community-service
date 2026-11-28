package com.manacommunity.api.edgehardware.repository;

import com.manacommunity.api.edgehardware.entity.EdgeBarrierCommandNonce;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EdgeBarrierCommandNonceRepository extends JpaRepository<EdgeBarrierCommandNonce, Long> {
    Optional<EdgeBarrierCommandNonce> findByControllerIdAndCmdNonce(String controllerId, String cmdNonce);
}
