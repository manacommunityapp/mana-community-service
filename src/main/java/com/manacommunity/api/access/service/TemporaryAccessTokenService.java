package com.manacommunity.api.access.service;

import com.manacommunity.api.access.entity.TemporaryAccessToken;
import com.manacommunity.api.access.repository.TemporaryAccessTokenRepository;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemporaryAccessTokenService {

    private final TemporaryAccessTokenRepository repo;

    @Transactional
    public TemporaryAccessToken createPass(Long communityId, Long userId, String purpose,
                                           String visitorName, LocalDateTime validFrom,
                                           LocalDateTime validUntil, Integer maxUses) {
        return repo.save(TemporaryAccessToken.builder()
                .communityId(communityId)
                .createdByUserId(userId)
                .tokenCode(UUID.randomUUID().toString().replace("-", ""))
                .purpose(purpose)
                .visitorName(visitorName)
                .validFrom(validFrom != null ? validFrom : LocalDateTime.now())
                .validUntil(validUntil)
                .maxUses(maxUses != null ? maxUses : 1)
                .build());
    }

    @Transactional(readOnly = true)
    public List<TemporaryAccessToken> getMyPasses(Long userId) {
        return repo.findByCreatedByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public TemporaryAccessToken verifyPass(String tokenCode) {
        TemporaryAccessToken token = repo.findByTokenCode(tokenCode)
                .orElseThrow(() -> new ResourceNotFoundException("TemporaryAccessToken", "code", tokenCode));
        if (!"ACTIVE".equals(token.getStatus())) {
            throw new InvalidInputException("Pass is not active");
        }
        if (LocalDateTime.now().isAfter(token.getValidUntil())) {
            token.setStatus("EXPIRED");
            repo.save(token);
            throw new InvalidInputException("Pass has expired");
        }
        if (token.getUseCount() >= token.getMaxUses()) {
            throw new InvalidInputException("Pass has been fully used");
        }
        token.setUseCount(token.getUseCount() + 1);
        if (token.getUseCount() >= token.getMaxUses()) {
            token.setStatus("USED");
        }
        return repo.save(token);
    }

    @Transactional
    public void revokePass(Long id) {
        repo.findById(id).ifPresent(t -> {
            t.setStatus("REVOKED");
            repo.save(t);
        });
    }
}
