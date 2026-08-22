package com.manacommunity.api.slice.controller;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.support.BaseWebMvcTest;
import com.manacommunity.api.user.controller.UserController;
import com.manacommunity.api.user.dto.AdminCreateUserRequest;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.service.AdminUserService;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.repository.RolePermissionRepository;
import com.manacommunity.api.service.RoleService;
import com.manacommunity.api.service.CommunityModuleService;
import com.manacommunity.api.user.service.MenuRolePermissionService;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController - User Creation & Bulk Upload Slice Tests")
class UserControllerTest extends BaseWebMvcTest {

    @MockitoBean private LoggedInUserService loggedInUserService;
    @MockitoBean private RoleService roleService;
    @MockitoBean private CommunityModuleService communityModuleService;
    @MockitoBean private MenuRolePermissionService menuRolePermissionService;
    @MockitoBean private AdminUserService adminUserService;
    @MockitoBean private PasswordEncoder passwordEncoder;
    @MockitoBean private AuthService authService;

    @Nested
    @DisplayName("POST /api/users - Create User (Single & Bulk Row Insertion)")
    class CreateUserEndpoint {

        @Test
        @DisplayName("valid user creation request returns 200 with UserResponse")
        void valid_returns200() throws Exception {
            AdminCreateUserRequest req = new AdminCreateUserRequest();
            req.setFirstName("Priya");
            req.setLastName("Sharma");
            req.setEmail("priya.sharma@example.com");
            req.setPhone("+91 98765 43210");
            req.setRole("member");
            req.setInviteCode("APT-TOWER-A-2024");
            req.setFlatNo("Apt 402");
            req.setGovtIdType("Aadhaar Card");
            req.setGovtIdNumber("XXXX-XXXX-1234");

            Community community = Community.builder().id(1L).name("Test Community").inviteCode("APT-TOWER-A-2024").build();
            Role role = Role.builder().id(10L).name("MEMBER").communityId(1L).permissions(new HashSet<>()).build();

            AppUser mockSaved = AppUser.builder()
                    .id(101L)
                    .fullName("Priya Sharma")
                    .email("priya.sharma@example.com")
                    .phone("+91 98765 43210")
                    .role("MEMBER, USER")
                    .roleEntity(role)
                    .kycStatus("VERIFIED")
                    .community(community)
                    .flatNo("Apt 402")
                    .isActive(true)
                    .dateOfBirth(LocalDate.of(2000, 1, 1))
                    .gender("OTHER")
                    .build();

            AppUser adminPrincipalUser = AppUser.builder()
                    .id(1L)
                    .email("admin@test.com")
                    .role("SUPER_ADMIN")
                    .build();

            when(loggedInUserService.resolve(any())).thenReturn(adminPrincipalUser);
            when(adminUserService.createUser(any())).thenReturn(mockSaved);
            when(rolePermissionRepository.findByUserId(any())).thenReturn(Collections.emptyList());

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(101))
                    .andExpect(jsonPath("$.fullName").value("Priya Sharma"))
                    .andExpect(jsonPath("$.email").value("priya.sharma@example.com"))
                    .andExpect(jsonPath("$.phone").value("+91 98765 43210"))
                    .andExpect(jsonPath("$.kycStatus").value("VERIFIED"))
                    .andExpect(jsonPath("$.flatNo").value("Apt 402"))
                    .andExpect(jsonPath("$.isActive").value(true));
        }

        @Test
        @DisplayName("missing mandatory field (e.g. email) returns 400 Bad Request")
        void missingEmail_returns400() throws Exception {
            AdminCreateUserRequest req = new AdminCreateUserRequest();
            req.setFirstName("Priya");
            req.setLastName("Sharma");
            req.setPhone("+91 98765 43210");
            // email is missing (@NotBlank constraint)

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isBadRequest());
        }
    }
}
