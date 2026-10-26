package com.manacommunity.api.family.service;

import com.manacommunity.api.family.dto.FamilyMemberRequest;
import com.manacommunity.api.family.repository.FamilyMemberRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.model.FamilyMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FamilyService {

    private final FamilyMemberRepository repo;

    public List<FamilyMember> getMyFamily(AppUser user) {
        return repo.findByUserIdAndStatusNot(user.getId(), "DELETED");
    }

    public FamilyMember addMember(FamilyMemberRequest req, AppUser user) {
        FamilyMember m = FamilyMember.builder()
                .user(user)
                .community(user.getCommunity())
                .name(req.getName())
                .relation(req.getRelation())
                .phone(req.getPhone())
                .dob(req.getDob())
                .age(req.getAge())
                .gender(req.getGender())
                .emergencyContact(Boolean.TRUE.equals(req.getEmergencyContact()))
                .status("ACTIVE")
                .build();
        return repo.save(m);
    }

    public FamilyMember updateMember(Long id, FamilyMemberRequest req, AppUser user) {
        FamilyMember m = repo.findById(id).orElseThrow(() -> new RuntimeException("Member not found"));
        if (!m.getUser().getId().equals(user.getId())) throw new RuntimeException("Not authorized");
        m.setName(req.getName());
        m.setRelation(req.getRelation());
        m.setPhone(req.getPhone());
        m.setDob(req.getDob());
        if (req.getAge() != null) m.setAge(req.getAge());
        if (req.getGender() != null) m.setGender(req.getGender());
        if (req.getEmergencyContact() != null) m.setEmergencyContact(req.getEmergencyContact());
        return repo.save(m);
    }

    public void removeMember(Long id, AppUser user) {
        FamilyMember m = repo.findById(id).orElseThrow(() -> new RuntimeException("Member not found"));
        if (!m.getUser().getId().equals(user.getId())) throw new RuntimeException("Not authorized");
        m.setStatus("DELETED");
        repo.save(m);
    }
}
