package com.manacommunity.api.access.service;

import com.manacommunity.api.access.entity.AccessCard;
import com.manacommunity.api.access.repository.AccessCardRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccessCardService {

    private final AccessCardRepository repo;

    @Transactional(readOnly = true)
    public List<AccessCard> getCards(Long communityId, Long userId) {
        if (userId != null) {
            return repo.findByCommunityIdAndUserIdOrderByCreatedAtDesc(communityId, userId);
        }
        return repo.findByCommunityIdOrderByCreatedAtDesc(communityId);
    }

    @Transactional
    public AccessCard issueCard(AccessCard card) {
        return repo.save(card);
    }

    @Transactional
    public AccessCard updateStatus(Long id, String status, String reason) {
        AccessCard card = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AccessCard", id));
        card.setStatus(status);
        if (reason != null) card.setDeactivatedReason(reason);
        return repo.save(card);
    }

    @Transactional
    public void deactivateCard(Long id) {
        updateStatus(id, "INACTIVE", "Manually deactivated");
    }
}
