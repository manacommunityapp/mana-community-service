package com.manacommunity.api.health.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DoctorVerificationEngine {
    public enum VerificationStatus { SUBMITTED, UNDER_REVIEW, VERIFIED, REJECTED }

    public static class DoctorProfile {
        public String id;
        public String name;
        public String registrationNumber;
        public String medicalCouncil;
        public int experienceYears;
        public VerificationStatus status;
        public boolean verifiedBadge;
        public int verifiedCommunityConsultations;
    }

    private final Map<String, DoctorProfile> doctorRegistry = new ConcurrentHashMap<>();

    public DoctorProfile registerDoctor(String id, String name, String regNum, String council, int exp) {
        if (regNum == null || regNum.trim().isEmpty()) {
            throw new IllegalArgumentException("Medical registration number is mandatory");
        }
        DoctorProfile doc = new DoctorProfile();
        doc.id = id;
        doc.name = name;
        doc.registrationNumber = regNum;
        doc.medicalCouncil = council;
        doc.experienceYears = exp;
        doc.status = VerificationStatus.SUBMITTED;
        doc.verifiedBadge = false;
        doc.verifiedCommunityConsultations = 0;
        doctorRegistry.put(id, doc);
        return doc;
    }

    public DoctorProfile verifyDoctor(String id, boolean approved) {
        DoctorProfile doc = doctorRegistry.get(id);
        if (doc == null) throw new IllegalArgumentException("Doctor not found: " + id);
        if (approved) {
            doc.status = VerificationStatus.VERIFIED;
            doc.verifiedBadge = true;
        } else {
            doc.status = VerificationStatus.REJECTED;
            doc.verifiedBadge = false;
        }
        return doc;
    }

    public void recordCommunityConsultation(String id) {
        DoctorProfile doc = doctorRegistry.get(id);
        if (doc != null) {
            doc.verifiedCommunityConsultations++;
        }
    }

    public DoctorProfile getDoctor(String id) {
        return doctorRegistry.get(id);
    }
}