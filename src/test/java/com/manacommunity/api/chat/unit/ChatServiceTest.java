package com.manacommunity.api.chat.unit;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.sports.controller.*;

import com.manacommunity.api.dto.chat.ChatMessageResponse;
import com.manacommunity.api.dto.chat.ConversationResponse;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.model.ChatMessage;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Conversation;
import com.manacommunity.api.model.ConversationParticipant;
import com.manacommunity.api.repository.ChatMessageRepository;
import com.manacommunity.api.repository.ConversationParticipantRepository;
import com.manacommunity.api.repository.ConversationRepository;
import com.manacommunity.api.service.ChatService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Chat Service Unit Tests")
class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationParticipantRepository participantRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @InjectMocks
    private ChatService chatService;

    private Community testCommunity;
    private AppUser user1;
    private AppUser user2;
    private Conversation conversation;
    private ConversationParticipant participant1;

    @BeforeEach
    void setUp() {
        testCommunity = Community.builder().id(1L).name("Mana Residency").build();
        user1 = AppUser.builder().id(10L).fullName("Alice Smith").community(testCommunity).role("USER").kycStatus("VERIFIED").isActive(true).build();
        user2 = AppUser.builder().id(20L).fullName("Bob Jones").community(testCommunity).role("ADMIN").kycStatus("VERIFIED").isActive(true).build();

        conversation = Conversation.builder()
                .id(100L)
                .type("DIRECT")
                .community(testCommunity)
                .lastMessage("Hello")
                .lastMessageAt(LocalDateTime.now())
                .build();

        participant1 = ConversationParticipant.builder()
                .id(1L)
                .conversation(conversation)
                .user(user1)
                .lastReadAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should get conversations for current user")
    void shouldGetConversations() {
        when(participantRepository.findByUserIdOrderByConversationLastMessageAtDesc(10L))
                .thenReturn(List.of(participant1));
        when(participantRepository.findByConversationId(100L))
                .thenReturn(List.of(participant1, ConversationParticipant.builder().user(user2).conversation(conversation).build()));
        when(messageRepository.countUnread(eq(100L), any(), eq(10L))).thenReturn(0L);

        List<ConversationResponse> result = chatService.getConversations(user1);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("DIRECT", result.get(0).type());
    }

    @Test
    @DisplayName("Should send message and broadcast over WebSocket")
    void shouldSendMessageAndBroadcast() {
        when(participantRepository.findByConversationIdAndUserId(100L, 10L)).thenReturn(Optional.of(participant1));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage msg = inv.getArgument(0);
            msg.setId(500L);
            return msg;
        });
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);
        when(participantRepository.findByConversationId(100L)).thenReturn(List.of(participant1));

        ChatMessageResponse response = chatService.sendMessage(user1, 100L, "Hello Bob!");

        assertNotNull(response);
        assertEquals("Hello Bob!", response.content());
        assertEquals(10L, response.senderId());
        verify(messagingTemplate).convertAndSend(eq("/topic/conversation/100"), any(ChatMessageResponse.class));
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should throw exception on empty message content")
    void shouldThrowExceptionOnEmptyMessage() {
        assertThrows(InvalidInputException.class, () -> chatService.sendMessage(user1, 100L, "   "));
    }

    @Test
    @DisplayName("Should mark conversation as read")
    void shouldMarkConversationAsRead() {
        when(participantRepository.findByConversationIdAndUserId(100L, 10L)).thenReturn(Optional.of(participant1));
        when(participantRepository.save(any(ConversationParticipant.class))).thenAnswer(inv -> inv.getArgument(0));

        chatService.markRead(user1, 100L);
        assertNotNull(participant1.getLastReadAt());
        verify(participantRepository).save(participant1);
    }

    @Test
    @DisplayName("Should create group conversation and add creator and members")
    void shouldCreateGroupConversation() {
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(200L);
            return c;
        });
        when(participantRepository.save(any(ConversationParticipant.class))).thenAnswer(inv -> {
            ConversationParticipant p = inv.getArgument(0);
            p.setId(20L);
            return p;
        });
        when(userRepository.findById(20L)).thenReturn(Optional.of(user2));
        when(messageRepository.countUnread(eq(200L), any(), eq(10L))).thenReturn(0L);

        ConversationResponse group = chatService.createGroup(user1, "Tower A Committee", List.of(20L));

        assertNotNull(group);
        assertEquals("GROUP", group.type());
        assertEquals("Tower A Committee", group.title());
        assertTrue(group.isGroup());
    }

    @Test
    @DisplayName("Should add new members to existing group")
    void shouldAddGroupMembers() {
        Conversation groupConv = Conversation.builder().id(200L).type("GROUP").title("Sports Club").build();
        when(participantRepository.findByConversationIdAndUserId(200L, 10L)).thenReturn(Optional.of(participant1));
        when(conversationRepository.findById(200L)).thenReturn(Optional.of(groupConv));
        when(participantRepository.findByConversationIdAndUserId(200L, 20L)).thenReturn(Optional.empty());
        when(userRepository.findById(20L)).thenReturn(Optional.of(user2));

        chatService.addGroupMembers(user1, 200L, List.of(20L));

        verify(participantRepository).save(any(ConversationParticipant.class));
    }

    @Test
    @DisplayName("Should remove member from group")
    void shouldRemoveGroupMember() {
        ConversationParticipant memberToRemove = ConversationParticipant.builder().id(30L).user(user2).build();
        when(participantRepository.findByConversationIdAndUserId(200L, 10L)).thenReturn(Optional.of(participant1));
        when(participantRepository.findByConversationIdAndUserId(200L, 20L)).thenReturn(Optional.of(memberToRemove));

        chatService.removeGroupMember(user1, 200L, 20L);

        verify(participantRepository).delete(memberToRemove);
    }
}
