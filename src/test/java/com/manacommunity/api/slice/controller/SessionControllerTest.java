package com.manacommunity.api.slice.controller;

import com.manacommunity.api.repository.AuditLogRepository;
import com.manacommunity.api.support.BaseWebMvcTest;
import com.manacommunity.api.user.controller.SessionController;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.UserSessionRepository;
import com.manacommunity.api.user.security.SessionService;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SessionController - Paginated Security Audit Slice Tests")
class SessionControllerTest extends BaseWebMvcTest {

    @MockitoBean
    private UserSessionRepository sessionRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private SessionService sessionService;

    @MockitoBean
    private LoggedInUserService loggedInUserService;

    @Test
    @DisplayName("GET /api/user/security-audit returns paginated PagedResponse with page and size")
    void getSecurityAudit_returnsPagedResponse() throws Exception {
        AppUser user = AppUser.builder().id(1L).fullName("Test User").email("test@example.com").build();
        when(loggedInUserService.resolve(any())).thenReturn(user);
        when(sessionRepository.findByUserIdOrderByLoginAtDesc(any(), any())).thenReturn(Collections.emptyList());
        when(auditLogRepository.search(any(), any(), any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/user/security-audit")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.content").isArray());
    }
}
