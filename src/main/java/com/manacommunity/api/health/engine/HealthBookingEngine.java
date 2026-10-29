package com.manacommunity.api.health.engine;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HealthBookingEngine {
    public enum ConsultationMode { IN_PERSON, VIDEO, PHONE }
    public enum AppointmentStatus { REQUESTED, CONFIRMED, COMPLETED, CANCELLED, RESCHEDULED }

    private final Set<String> bookedSlots = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, AppointmentRecord> appointments = new ConcurrentHashMap<>();

    public static class AppointmentRecord {
        public String id;
        public String doctorId;
        public String patientId;
        public String patientName;
        public String bookedByUserId;
        public String slotTime;
        public ConsultationMode mode;
        public AppointmentStatus status;
        public double fee;
        public List<String> statusHistory = new ArrayList<>();
    }

    public synchronized AppointmentRecord bookSlot(
            String apptId, String doctorId, String patientId, String patientName,
            String bookedByUserId, String slotTime, ConsultationMode mode, double fee) {
        String slotKey = doctorId + "@" + slotTime;
        if (bookedSlots.contains(slotKey)) {
            throw new IllegalStateException("Slot is already booked for doctor " + doctorId + " at " + slotTime);
        }
        bookedSlots.add(slotKey);

        AppointmentRecord appt = new AppointmentRecord();
        appt.id = apptId;
        appt.doctorId = doctorId;
        appt.patientId = patientId;
        appt.patientName = patientName;
        appt.bookedByUserId = bookedByUserId;
        appt.slotTime = slotTime;
        appt.mode = mode;
        appt.status = AppointmentStatus.CONFIRMED;
        appt.fee = fee;
        appt.statusHistory.add("CONFIRMED at " + LocalDateTime.now());

        appointments.put(apptId, appt);
        return appt;
    }

    public synchronized AppointmentRecord cancelAppointment(String apptId, String reason) {
        AppointmentRecord appt = appointments.get(apptId);
        if (appt == null) throw new IllegalArgumentException("Appointment not found: " + apptId);
        if (appt.status == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Appointment already cancelled");
        }
        String slotKey = appt.doctorId + "@" + appt.slotTime;
        bookedSlots.remove(slotKey);
        appt.status = AppointmentStatus.CANCELLED;
        appt.statusHistory.add("CANCELLED: " + reason + " at " + LocalDateTime.now());
        return appt;
    }

    public AppointmentRecord getAppointment(String apptId) {
        return appointments.get(apptId);
    }
}