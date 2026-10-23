package com.manacommunity.api.health.unit;

import com.manacommunity.api.health.engine.DoctorVerificationEngine;
import com.manacommunity.api.health.engine.DoctorVerificationEngine.DoctorProfile;
import com.manacommunity.api.health.engine.DoctorVerificationEngine.VerificationStatus;
import com.manacommunity.api.health.engine.HealthBookingEngine;
import com.manacommunity.api.health.engine.HealthBookingEngine.AppointmentRecord;
import com.manacommunity.api.health.engine.HealthBookingEngine.ConsultationMode;
import com.manacommunity.api.health.engine.HealthPrivacyAuditEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mana Health Engine Unit Tests")
class HealthEngineTest {

    @Test
    void testDoctorVerificationLifecycle() {
        DoctorVerificationEngine engine = new DoctorVerificationEngine();
        DoctorProfile doc = engine.registerDoctor("doc-1", "Dr. Rajesh Kumar", "KMC-48291", "Karnataka Medical Council", 15);
        assertEquals(VerificationStatus.SUBMITTED, doc.status);
        assertFalse(doc.verifiedBadge);

        engine.verifyDoctor("doc-1", true);
        DoctorProfile verified = engine.getDoctor("doc-1");
        assertEquals(VerificationStatus.VERIFIED, verified.status);
        assertTrue(verified.verifiedBadge);

        engine.recordCommunityConsultation("doc-1");
        assertEquals(1, verified.verifiedCommunityConsultations);
    }

    @Test
    void testAntiDoubleBookingLock() {
        HealthBookingEngine engine = new HealthBookingEngine();
        AppointmentRecord appt = engine.bookSlot(
            "appt-1", "doc-1", "pat-1", "Sandeep Roy", "user-1", "2026-10-10T18:30:00", ConsultationMode.IN_PERSON, 700.0
        );
        assertNotNull(appt);

        assertThrows(IllegalStateException.class, () -> {
            engine.bookSlot(
                "appt-2", "doc-1", "pat-2", "Other Patient", "user-2", "2026-10-10T18:30:00", ConsultationMode.VIDEO, 700.0
            );
        });
    }

    @Test
    void testPrivacyAccessAndAuditing() {
        HealthPrivacyAuditEngine engine = new HealthPrivacyAuditEngine();
        assertTrue(engine.checkAndAuditAccess("user-1", "RESIDENT", "user-1", "rec-101", "VIEW_PRESCRIPTION"));
        assertFalse(engine.checkAndAuditAccess("admin-1", "COMMUNITY_ADMIN", "user-1", "rec-101", "VIEW_PRESCRIPTION"));

        engine.grantAccess("user-1", "doc-1");
        assertTrue(engine.checkAndAuditAccess("doc-1", "DOCTOR", "user-1", "rec-101", "VIEW_PRESCRIPTION"));
        assertEquals(3, engine.getAuditTrailForPatient("user-1").size());
    }
}