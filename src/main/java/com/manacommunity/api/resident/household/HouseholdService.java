package com.manacommunity.api.resident.household;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.VehicleProfile;
import com.manacommunity.api.repository.VehicleProfileRepository;
import com.manacommunity.api.resident.model.CommunityFlat;
import com.manacommunity.api.resident.model.FlatDependentMember;
import com.manacommunity.api.resident.model.FlatMembership;
import com.manacommunity.api.resident.repository.CommunityFlatRepository;
import com.manacommunity.api.resident.repository.FlatDependentMemberRepository;
import com.manacommunity.api.resident.repository.FlatMembershipRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HouseholdService {

    private final FlatMembershipRepository membershipRepo;
    private final FlatDependentMemberRepository dependentRepo;
    private final CommunityFlatRepository flatRepo;
    private final VehicleProfileRepository vehicleRepo;
    private final HouseholdStaffLinkRepository staffRepo;

    /** Returns the full household graph for the calling user's flat. */
    public HouseholdResponse getMyHousehold(AppUser user) {
        List<FlatMembership> memberships = membershipRepo.findByUserIdWithFlatAndCommunity(user.getId());
        if (memberships.isEmpty()) {
            throw new ResourceNotFoundException("No household found for user " + user.getId());
        }
        // Prefer the flat where user is primary resident; fall back to first membership
        FlatMembership primary = memberships.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsPrimaryResident()))
                .findFirst()
                .orElse(memberships.get(0));
        return buildResponse(primary.getFlat(), user);
    }

    /** Returns the household for a specific flat, with community membership guard. */
    public HouseholdResponse getHousehold(Long flatId, AppUser requestingUser) {
        CommunityFlat flat = flatRepo.findById(flatId)
                .orElseThrow(() -> new ResourceNotFoundException("Flat not found: " + flatId));

        boolean isAdmin = isAdminOrFM(requestingUser);
        boolean isMember = membershipRepo.findByFlatId(flatId).stream()
                .anyMatch(m -> m.getUser() != null && m.getUser().getId().equals(requestingUser.getId()));

        if (!isAdmin && !isMember) {
            throw new ResourceNotFoundException("Flat not found: " + flatId); // 404 is safer than 403
        }
        return buildResponse(flat, requestingUser);
    }

    /** Returns all user IDs sharing a household with this user (for notification fan-out etc). */
    public List<Long> getFamilyMemberIds(Long userId) {
        return membershipRepo.findByUserId(userId).stream()
                .flatMap(m -> membershipRepo.findByFlatId(m.getFlat().getId()).stream())
                .map(m -> m.getUser() != null ? m.getUser().getId() : null)
                .filter(id -> id != null && !id.equals(userId))
                .distinct()
                .toList();
    }

    public boolean isSameHousehold(Long userAId, Long userBId) {
        List<Long> flatIdsA = membershipRepo.findByUserId(userAId).stream()
                .map(m -> m.getFlat().getId()).toList();
        return membershipRepo.findByUserId(userBId).stream()
                .map(m -> m.getFlat().getId())
                .anyMatch(flatIdsA::contains);
    }

    @Transactional
    public HouseholdStaffLink addStaff(Long flatId, StaffRequest req, AppUser user) {
        CommunityFlat flat = flatRepo.findById(flatId)
                .orElseThrow(() -> new ResourceNotFoundException("Flat not found: " + flatId));
        assertFlatAccess(flatId, user);
        HouseholdStaffLink link = HouseholdStaffLink.builder()
                .flat(flat)
                .staffName(req.getStaffName())
                .staffType(req.getStaffType())
                .phone(req.getPhone())
                .sinceDate(req.getSinceDate())
                .build();
        return staffRepo.save(link);
    }

    @Transactional
    public void removeStaff(Long linkId, AppUser user) {
        HouseholdStaffLink link = staffRepo.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff link not found: " + linkId));
        assertFlatAccess(link.getFlat().getId(), user);
        link.setActive(false);
        staffRepo.save(link);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private HouseholdResponse buildResponse(CommunityFlat flat, AppUser requestingUser) {
        List<FlatMembership> members   = membershipRepo.findByFlatIdWithUser(flat.getId());
        List<FlatDependentMember> deps = dependentRepo.findByFlatId(flat.getId());
        List<VehicleProfile> vehicles  = vehicleRepo.findAll().stream()
                .filter(v -> flat.equals(v.getFlat())).toList();
        List<HouseholdStaffLink> staff = staffRepo.findByFlatIdAndActiveTrue(flat.getId());

        return HouseholdResponse.builder()
                .flatId(flat.getId())
                .towerBlock(flat.getTowerBlock())
                .flatNumber(flat.getFlatNumber())
                .floorNumber(flat.getFloorNumber())
                .communityId(flat.getCommunity().getId())
                .communityName(flat.getCommunity().getName())
                .verificationStatus(flat.getVerificationStatus())
                .adults(members.stream().map(this::toMemberDto).toList())
                .dependents(deps.stream().map(this::toDependentDto).toList())
                .vehicles(vehicles.stream().map(this::toVehicleDto).toList())
                .staff(staff.stream().map(this::toStaffDto).toList())
                .build();
    }

    private void assertFlatAccess(Long flatId, AppUser user) {
        if (isAdminOrFM(user)) return;
        boolean isMember = membershipRepo.findByFlatId(flatId).stream()
                .anyMatch(m -> m.getUser() != null && m.getUser().getId().equals(user.getId()));
        if (!isMember) throw new ResourceNotFoundException("Flat not found: " + flatId);
    }

    private boolean isAdminOrFM(AppUser user) {
        String role = user.getRole();
        return role != null && (role.contains("ADMIN") || role.contains("FM") || role.contains("MANAGER"));
    }

    private HouseholdResponse.MemberDto toMemberDto(FlatMembership m) {
        AppUser u = m.getUser();
        return HouseholdResponse.MemberDto.builder()
                .membershipId(m.getId())
                .userId(u != null ? u.getId() : null)
                .fullName(u != null ? u.getFullName() : null)
                .phone(u != null ? u.getPhone() : null)
                .email(u != null ? u.getEmail() : null)
                .residentType(m.getResidentType())
                .relationship(m.getRelationshipToFlat())
                .isPrimary(m.getIsPrimaryResident())
                .accessStatus(m.getAppAccessStatus())
                .build();
    }

    private HouseholdResponse.DependentDto toDependentDto(FlatDependentMember d) {
        return HouseholdResponse.DependentDto.builder()
                .dependentId(d.getId())
                .fullName(d.getFullName())
                .dateOfBirth(d.getDateOfBirth())
                .gender(d.getGender())
                .relationship(d.getRelationship())
                .sportsEligible(d.getSportsEligible())
                .status(d.getStatus())
                .build();
    }

    private HouseholdResponse.VehicleDto toVehicleDto(VehicleProfile v) {
        return HouseholdResponse.VehicleDto.builder()
                .vehicleId(v.getId())
                .vehicleType(v.getVehicleType())
                .make(v.getMake())
                .model(v.getModel())
                .color(v.getColor())
                .plateNumber(v.getPlateNumber())
                .rfidTag(v.getRfidTag())
                .parkingSlot(v.getParkingSlot())
                .isDefault(v.isDefault())
                .isActive(v.isActive())
                .build();
    }

    private HouseholdResponse.StaffDto toStaffDto(HouseholdStaffLink s) {
        return HouseholdResponse.StaffDto.builder()
                .linkId(s.getId())
                .staffName(s.getStaffName())
                .staffType(s.getStaffType())
                .phone(s.getPhone())
                .sinceDate(s.getSinceDate())
                .active(s.getActive())
                .build();
    }
}
