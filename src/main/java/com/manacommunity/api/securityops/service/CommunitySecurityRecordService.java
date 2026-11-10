package com.manacommunity.api.securityops.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.securityops.dto.SecurityRecordRequest;
import com.manacommunity.api.securityops.dto.SecurityRecordResponse;
import com.manacommunity.api.securityops.entity.CommunitySecurityRecord;
import com.manacommunity.api.securityops.repository.CommunitySecurityRecordRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service @RequiredArgsConstructor
public class CommunitySecurityRecordService {
    private final CommunitySecurityRecordRepository repository;
    private boolean elevated(AppUser u) { String role = u.getRole() == null ? "" : u.getRole().toUpperCase(); return role.contains("ADMIN") || role.contains("GUARD") || role.contains("SECURITY"); }
    @Transactional(readOnly = true) public List<SecurityRecordResponse> list(AppUser user, String type) {
        List<CommunitySecurityRecord> records = elevated(user) ? repository.findByCommunityIdAndRecordTypeOrderByCreatedAtDesc(user.getCommunity().getId(), type) : repository.findByCommunityIdAndRecordTypeAndResidentIdOrderByCreatedAtDesc(user.getCommunity().getId(), type, user.getId());
        return records.stream().map(r -> SecurityRecordResponse.from(r, null)).toList();
    }
    @Transactional public SecurityRecordResponse create(AppUser user, String type, SecurityRecordRequest q, boolean residentOwned, boolean otp) {
        if ((type.equals("BLACKLIST") || type.startsWith("GUARD_" ) || type.equals("SHIFT_HANDOVER")) && !elevated(user)) throw new SecurityException("Guard or administrator role required");
        String generated = otp ? String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000)) : null;
        CommunitySecurityRecord r = CommunitySecurityRecord.builder().community(user.getCommunity()).actor(user).resident(residentOwned ? user : null).recordType(type).name(q.name()).phone(q.phone()).vehicleNumber(q.vehicleNumber()).validFrom(q.validFrom()).validUntil(q.validUntil()).timeWindow(q.timeWindow()).purpose(q.purpose()).location(q.location()).status(q.status() == null ? "ACTIVE" : q.status()).notes(q.notes()).otpHash(generated == null ? null : hash(generated)).otpExpiresAt(generated == null ? null : LocalDateTime.now().plusHours(24)).build();
        return SecurityRecordResponse.from(repository.save(r), generated);
    }
    @Transactional public SecurityRecordResponse update(AppUser user, Long id, String type, SecurityRecordRequest q) {
        CommunitySecurityRecord r = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("SecurityRecord", id));
        if (!r.getCommunity().getId().equals(user.getCommunity().getId()) || !type.equals(r.getRecordType()) || (!elevated(user) && (r.getResident() == null || !r.getResident().getId().equals(user.getId())))) throw new ResourceNotFoundException("SecurityRecord", id);
        r.setName(q.name()); r.setPhone(q.phone()); r.setVehicleNumber(q.vehicleNumber()); r.setValidFrom(q.validFrom()); r.setValidUntil(q.validUntil()); r.setTimeWindow(q.timeWindow()); r.setPurpose(q.purpose()); r.setLocation(q.location()); r.setStatus(q.status() == null ? r.getStatus() : q.status()); r.setNotes(q.notes()); return SecurityRecordResponse.from(repository.save(r), null);
    }
    @Transactional public void delete(AppUser user, Long id, String type) { CommunitySecurityRecord r = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("SecurityRecord", id)); if (!r.getCommunity().getId().equals(user.getCommunity().getId()) || !type.equals(r.getRecordType()) || (!elevated(user) && (r.getResident() == null || !r.getResident().getId().equals(user.getId())))) throw new ResourceNotFoundException("SecurityRecord", id); repository.delete(r); }
    @Transactional public SecurityRecordResponse collect(AppUser user, Long id, String otp) { CommunitySecurityRecord r = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("SecurityRecord", id)); if (!r.getCommunity().getId().equals(user.getCommunity().getId()) || !"PACKAGE".equals(r.getRecordType()) || (r.getResident() == null || !r.getResident().getId().equals(user.getId()))) throw new ResourceNotFoundException("SecurityRecord", id); if (r.getOtpExpiresAt() == null || r.getOtpExpiresAt().isBefore(LocalDateTime.now()) || !hash(otp == null ? "" : otp).equals(r.getOtpHash())) throw new IllegalArgumentException("Invalid or expired collection OTP"); r.setStatus("COLLECTED"); return SecurityRecordResponse.from(repository.save(r), null); }
    private String hash(String value) { try { byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder b = new StringBuilder(); for (byte x : bytes) b.append(String.format("%02x", x)); return b.toString(); } catch (Exception e) { throw new IllegalStateException(e); } }
}
