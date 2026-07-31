package com.cpn.application.communication;

import com.cpn.domain.communication.model.ChatMessage;
import com.cpn.domain.communication.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;

    @Transactional(readOnly = true)
    public List<ChatMessage> getConversation(UUID u1, UUID u2) {
        return chatMessageRepository.findConversationBetween(u1, u2);
    }

    @Transactional
    public ChatMessage sendMessage(UUID senderId, UUID recipientId, String content) {
        ChatMessage msg = ChatMessage.builder()
                .senderId(senderId)
                .recipientId(recipientId)
                .content(content)
                .isRead(false)
                .build();
        return chatMessageRepository.save(msg);
    }
}
