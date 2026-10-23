package com.manacommunity.api.unit;

import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.model.FamilyMember;
import com.manacommunity.api.user.repository.FamilyMemberRepository;
import com.manacommunity.api.user.service.impl.FamilyMemberServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * IDOR protection tests for FamilyMember — verifies that users cannot modify
 * or delete family members belonging to another user.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyMember IDOR Protection")
class FamilyMemberIdorTest {

    @Mock
    private FamilyMemberRepository repository;

    @Mock
    private com.manacommunity.api.repository.CommunityRepository communityRepository;

    @InjectMocks
    private FamilyMemberServiceImpl service;

    private AppUser ownerUser;
    private AppUser attackerUser;
    private AppUser adminUser;
    private FamilyMember familyMember;

    @BeforeEach
    void setUp() {
        Community community = Community.builder().id(10L).build();

        ownerUser = new AppUser();
        ownerUser.setId(1L);
        ownerUser.setRole("MEMBER");
        ownerUser.setCommunity(community);

        attackerUser = new AppUser();
        attackerUser.setId(99L);
        attackerUser.setRole("MEMBER");
        attackerUser.setCommunity(community);

        adminUser = new AppUser();
        adminUser.setId(5L);
        adminUser.setRole("ADMIN");
        adminUser.setCommunity(community);

        familyMember = FamilyMember.builder()
                .id(42L)
                .user(ownerUser)
                .name("Test Member")
                .relation("Spouse")
                .build();
    }

    @Nested
    @DisplayName("updateFamilyMember")
    class Update {

        @Test
        @DisplayName("owner can update their own family member")
        void ownerCanUpdate() {
            when(repository.findById(42L)).thenReturn(Optional.of(familyMember));
            when(repository.save(any())).thenReturn(familyMember);

            service.updateFamilyMember(42L, familyMember, ownerUser, 10L);

            verify(repository).save(familyMember);
        }

        @Test
        @DisplayName("admin can update any family member")
        void adminCanUpdate() {
            when(repository.findById(42L)).thenReturn(Optional.of(familyMember));
            when(repository.save(any())).thenReturn(familyMember);

            service.updateFamilyMember(42L, familyMember, adminUser, 10L);

            verify(repository).save(familyMember);
        }

        @Test
        @DisplayName("non-owner cannot update another user's family member (IDOR blocked)")
        void nonOwnerCannotUpdate() {
            when(repository.findById(42L)).thenReturn(Optional.of(familyMember));

            assertThatThrownBy(() ->
                    service.updateFamilyMember(42L, familyMember, attackerUser, 10L))
                    .isInstanceOf(UnauthorizedActionException.class)
                    .hasMessageContaining("only modify your own family members");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteFamilyMember")
    class Delete {

        @Test
        @DisplayName("owner can delete their own family member")
        void ownerCanDelete() {
            when(repository.findById(42L)).thenReturn(Optional.of(familyMember));

            service.deleteFamilyMember(42L, ownerUser, 10L);

            verify(repository).delete(familyMember);
        }

        @Test
        @DisplayName("admin can delete any family member")
        void adminCanDelete() {
            when(repository.findById(42L)).thenReturn(Optional.of(familyMember));

            service.deleteFamilyMember(42L, adminUser, 10L);

            verify(repository).delete(familyMember);
        }

        @Test
        @DisplayName("attacker cannot delete another user's family member (IDOR blocked)")
        void attackerCannotDelete() {
            when(repository.findById(42L)).thenReturn(Optional.of(familyMember));

            assertThatThrownBy(() ->
                    service.deleteFamilyMember(42L, attackerUser, 10L))
                    .isInstanceOf(UnauthorizedActionException.class)
                    .hasMessageContaining("only delete your own family members");

            verify(repository, never()).delete(any(FamilyMember.class));
        }

        @Test
        @DisplayName("deleting non-existent member throws IllegalArgumentException")
        void deleteMissingMember() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.deleteFamilyMember(999L, ownerUser, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Family member not found");
        }
    }
}
