package com.manacommunity.api.unit;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.sports.repository.SportsEventRegistrationRepository;
import com.manacommunity.api.user.dto.FamilyMemberRequest;
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
 * or delete family members belonging to another user, and verifies tournament DOB locks.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyMember IDOR Protection & Tournament DOB Lock")
class FamilyMemberIdorTest {

    @Mock
    private FamilyMemberRepository repository;

    @Mock
    private com.manacommunity.api.repository.CommunityRepository communityRepository;

    @Mock
    private SportsEventRegistrationRepository sportsEventRegistrationRepository;

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
                .community(community)
                .name("Test Member")
                .relation("Spouse")
                .dob("2010-01-01")
                .age(14)
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
                    .hasMessageContaining("You are not authorized to update this family member");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("owner cannot update DOB when family member has active tournament registration")
        void ownerCannotUpdateDobWhenActiveTournamentsExist() {
            when(repository.findById(42L)).thenReturn(Optional.of(familyMember));
            when(sportsEventRegistrationRepository.existsActiveRegistrationForFamilyMember(eq(42L), any())).thenReturn(true);

            FamilyMemberRequest req = FamilyMemberRequest.builder()
                    .dob("2012-05-15")
                    .build();

            assertThatThrownBy(() ->
                    service.updateFamilyMember(42L, req, ownerUser, 10L))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("Date of birth cannot be modified while this family member has active registrations");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("owner can update DOB when family member has no active tournament registrations")
        void ownerCanUpdateDobWhenNoActiveTournaments() {
            when(repository.findById(42L)).thenReturn(Optional.of(familyMember));
            when(sportsEventRegistrationRepository.existsActiveRegistrationForFamilyMember(eq(42L), any())).thenReturn(false);
            when(repository.save(any())).thenReturn(familyMember);

            FamilyMemberRequest req = FamilyMemberRequest.builder()
                    .dob("2012-05-15")
                    .build();

            service.updateFamilyMember(42L, req, ownerUser, 10L);

            verify(repository).save(familyMember);
        }

        @Test
        @DisplayName("admin can update DOB even if family member has active tournament registration")
        void adminCanUpdateDobEvenWithActiveTournaments() {
            when(repository.findById(42L)).thenReturn(Optional.of(familyMember));
            when(repository.save(any())).thenReturn(familyMember);

            FamilyMemberRequest req = FamilyMemberRequest.builder()
                    .dob("2012-05-15")
                    .build();

            service.updateFamilyMember(42L, req, adminUser, 10L);

            verify(repository).save(familyMember);
            verify(sportsEventRegistrationRepository, never()).existsActiveRegistrationForFamilyMember(any(), any());
        }

        @Test
        @DisplayName("owner can set DOB for the first time even if registered")
        void ownerCanSetDobFirstTime() {
            familyMember.setDob(null);
            when(repository.findById(42L)).thenReturn(Optional.of(familyMember));
            when(repository.save(any())).thenReturn(familyMember);

            FamilyMemberRequest req = FamilyMemberRequest.builder()
                    .dob("2012-05-15")
                    .build();

            service.updateFamilyMember(42L, req, ownerUser, 10L);

            verify(repository).save(familyMember);
            verify(sportsEventRegistrationRepository, never()).existsActiveRegistrationForFamilyMember(any(), any());
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
                    .hasMessageContaining("You are not authorized to delete this family member");

            verify(repository, never()).delete(any(FamilyMember.class));
        }

        @Test
        @DisplayName("deleting non-existent member throws ResourceNotFoundException")
        void deleteMissingMember() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.deleteFamilyMember(999L, ownerUser, 10L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
