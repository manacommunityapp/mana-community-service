package com.manacommunity.api.homeservice.service;

import com.manacommunity.api.homeservice.dto.HomeServiceChatMessageRequest;
import com.manacommunity.api.homeservice.model.entity.ContextualChatMessageEntity;
import com.manacommunity.api.homeservice.repository.ContextualChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service("homeServiceContextualChatService")
@RequiredArgsConstructor
public class HomeServiceContextualChatService {
    private final ContextualChatMessageRepository chatRepository;

    @Transactional
    public ContextualChatMessageEntity sendMessage(HomeServiceChatMessageRequest req) {
        ContextualChatMessageEntity msg = ContextualChatMessageEntity.builder()
                .id(UUID.randomUUID().toString())
                .bookingId(req.getBookingId())
                .workerId(req.getWorkerId())
                .residentUserId(req.getResidentUserId())
                .senderType(req.getSenderType())
                .senderName(req.getSenderName())
                .messageText(req.getMessageText())
                .createdAt(LocalDateTime.now())
                .build();
        return chatRepository.save(msg);
    }

    public List<ContextualChatMessageEntity> getChatHistory(String workerId, String residentUserId) {
        return chatRepository.findByWorkerIdAndResidentUserIdOrderByCreatedAtAsc(workerId, residentUserId);
    }
}
