package com.manacommunity.api.unit;

import com.manacommunity.api.security.AuditLogService;
import com.manacommunity.api.user.model.UserSession;
import com.manacommunity.api.user.repository.UserSessionRepository;
import com.manacommunity.api.user.security.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService")
class SessionServiceTest {

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private AuditLogService auditLog;

    @InjectMocks
    private SessionService sessionService;

    @Test
    @DisplayName("getActiveSessionsForUser returns active sessions ordered by loginAt desc")
    void getActiveSessionsForUser_returnsSessions() {
        UserSession s1 = UserSession.builder().id(1L).userId(10L).status(UserSession.ACTIVE).build();
        UserSession s2 = UserSession.builder().id(2L).userId(10L).status(UserSession.ACTIVE).build();
        when(sessionRepository.findByUserIdAndStatusOrderByLoginAtDesc(10L, UserSession.ACTIVE))
                .thenReturn(List.of(s1, s2));

        List<UserSession> result = sessionService.getActiveSessionsForUser(10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("revokeSession updates status to LOGGED_OUT and saves")
    void revokeSession_success() {
        UserSession s = UserSession.builder().id(5L).userId(10L).status(UserSession.ACTIVE).build();
        when(sessionRepository.findByIdAndUserId(5L, 10L)).thenReturn(Optional.of(s));

        boolean result = sessionService.revokeSession(10L, 5L);

        assertThat(result).isTrue();
        assertThat(s.getStatus()).isEqualTo(UserSession.LOGGED_OUT);
        assertThat(s.getLogoutAt()).isNotNull();
        verify(sessionRepository).save(s);
        verify(auditLog).record(eq(AuditLogService.Action.LOGOUT), eq(10L), any(String.class));
    }

    @Test
    @DisplayName("revokeOtherSessions revokes all sessions except keepSessionId")
    void revokeOtherSessions_success() {
        UserSession current = UserSession.builder().id(1L).userId(10L).status(UserSession.ACTIVE).build();
        UserSession other1 = UserSession.builder().id(2L).userId(10L).status(UserSession.ACTIVE).build();
        UserSession other2 = UserSession.builder().id(3L).userId(10L).status(UserSession.ACTIVE).build();

        when(sessionRepository.findByUserIdAndStatusOrderByLoginAtDesc(10L, UserSession.ACTIVE))
                .thenReturn(List.of(current, other1, other2));

        int count = sessionService.revokeOtherSessions(10L, 1L);

        assertThat(count).isEqualTo(2);
        assertThat(other1.getStatus()).isEqualTo(UserSession.LOGGED_OUT);
        assertThat(other2.getStatus()).isEqualTo(UserSession.LOGGED_OUT);
        assertThat(current.getStatus()).isEqualTo(UserSession.ACTIVE);
        verify(sessionRepository).saveAll(any());
    }
}