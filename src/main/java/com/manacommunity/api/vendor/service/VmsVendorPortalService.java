package com.manacommunity.api.vendor.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.entity.*;
import com.manacommunity.api.vendor.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VmsVendorPortalService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> DAYS = List.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");
    private static final Set<String> DOC_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "doc", "docx");
    private static final long MAX_DOC_BYTES = 10L * 1024 * 1024;

    private final VmsVendorRepository vendorRepo;
    private final VmsVendorWorkingHoursRepository hoursRepo;
    private final VmsVendorHolidayRepository holidayRepo;
    private final VmsVendorLeaveRepository leaveRepo;
    private final VmsVendorFavoriteRepository favoriteRepo;
    private final VmsVendorDocumentRepository documentRepo;
    private final VmsVendorServiceRepository serviceRepo;

    @Value("${app.vendor.documents.upload-dir:uploads/vendor-documents}")
    private String uploadDir;

    private VmsVendor requireVendor(Long vendorId, Long communityId) {
        return vendorRepo.findByIdAndCommunityId(vendorId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", vendorId));
    }

    private VmsVendor requireMyVendor(AppUser user) {
        return vendorRepo.findByUserIdAndCommunityId(user.getId(), user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "userId", user.getId().toString()));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAvailability(Long vendorId, Long communityId) {
        VmsVendor vendor = requireVendor(vendorId, communityId);
        return availability(vendor);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyAvailability(AppUser user) {
        return availability(requireMyVendor(user));
    }

    private Map<String, Object> availability(VmsVendor vendor) {
        Map<String, Object> operatingHours = new LinkedHashMap<>();
        for (String day : DAYS) operatingHours.put(day, Map.of("isOpen", false));
        for (VmsVendorWorkingHours row : hoursRepo.findByVendorIdOrderByDayOfWeek(vendor.getId())) {
            String day = DAYS.get(Math.max(0, Math.min(6, row.getDayOfWeek() - 1)));
            operatingHours.put(day, Map.of("isOpen", Boolean.TRUE.equals(row.getIsWorkingDay()),
                    "openTime", row.getStartTime().format(TIME), "closeTime", row.getEndTime().format(TIME)));
        }
        List<Map<String, Object>> holidays = holidayRepo.findByVendorIdOrderByHolidayDate(vendor.getId()).stream()
                .map(h -> Map.<String, Object>of("id", h.getId(), "vendorId", vendor.getId(), "date", h.getHolidayDate().toString(),
                        "reason", h.getName() == null ? "" : h.getName(), "type", "HOLIDAY"))
                .toList();
        return Map.of("vendorId", vendor.getId(), "operatingHours", operatingHours, "holidays", holidays,
                "vacationMode", Boolean.TRUE.equals(vendor.getVacationMode()));
    }

    @Transactional
    public Map<String, Object> updateOperatingHours(AppUser user, Map<String, Object> hours) {
        VmsVendor vendor = requireMyVendor(user);
        hoursRepo.deleteByVendorId(vendor.getId());
        for (int i = 0; i < DAYS.size(); i++) {
            Object value = hours.get(DAYS.get(i));
            if (!(value instanceof Map<?, ?> schedule)) continue;
            boolean open = Boolean.TRUE.equals(schedule.get("isOpen"));
            String openTime = Objects.toString(schedule.get("openTime"), "09:00");
            String closeTime = Objects.toString(schedule.get("closeTime"), "17:00");
            hoursRepo.save(VmsVendorWorkingHours.builder().vendor(vendor).dayOfWeek(i + 1)
                    .startTime(LocalTime.parse(openTime.length() == 5 ? openTime : openTime.substring(0, 5), TIME))
                    .endTime(LocalTime.parse(closeTime.length() == 5 ? closeTime : closeTime.substring(0, 5), TIME))
                    .isWorkingDay(open).build());
        }
        return availability(vendor);
    }

    @Transactional
    public Map<String, Object> addHoliday(AppUser user, Map<String, String> body) {
        VmsVendor vendor = requireMyVendor(user);
        LocalDate date = LocalDate.parse(Objects.requireNonNull(body.get("date"), "date is required"));
        VmsVendorHoliday saved = holidayRepo.save(VmsVendorHoliday.builder().vendor(vendor).holidayDate(date)
                .name(body.getOrDefault("reason", body.get("name"))).recurring(false).build());
        return Map.of("id", saved.getId(), "vendorId", vendor.getId(), "date", date.toString(),
                "reason", saved.getName() == null ? "" : saved.getName(), "type", "HOLIDAY");
    }

    @Transactional
    public void removeHoliday(AppUser user, Long holidayId) {
        VmsVendor vendor = requireMyVendor(user);
        VmsVendorHoliday holiday = holidayRepo.findByIdAndVendorId(holidayId, vendor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor holiday", holidayId));
        holidayRepo.delete(holiday);
    }

    @Transactional
    public Map<String, Object> setVacationMode(AppUser user, boolean enabled, String start, String end) {
        VmsVendor vendor = requireMyVendor(user);
        LocalDate from = start == null || start.isBlank() ? null : LocalDate.parse(start);
        LocalDate to = end == null || end.isBlank() ? null : LocalDate.parse(end);
        if (enabled && from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("Vacation end date must be on or after the start date");
        }
        vendor.setVacationMode(enabled);
        vendorRepo.save(vendor);
        List<VmsVendorLeave> vacationLeaves = leaveRepo.findByVendorIdAndReason(vendor.getId(), "Vacation mode");
        for (VmsVendorLeave leave : vacationLeaves) leave.setStatus("CANCELLED");
        if (enabled && from != null && to != null) {
            leaveRepo.save(VmsVendorLeave.builder().vendor(vendor).startDate(from).endDate(to).reason("Vacation mode").status("APPROVED").build());
        }
        return availability(vendor);
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableSlots(Long vendorId, Long serviceId, LocalDate date, Long communityId) {
        VmsVendor vendor = requireVendor(vendorId, communityId);
        if (Boolean.TRUE.equals(vendor.getVacationMode())) return List.of();
        VmsVendorService service = serviceRepo.findById(serviceId).orElseThrow(() -> new ResourceNotFoundException("Vendor service", serviceId));
        if (!service.getVendor().getId().equals(vendorId) || service.getStatus() != VmsVendorService.ServiceStatus.ACTIVE) return List.of();
        if (holidayRepo.findByVendorIdOrderByHolidayDate(vendorId).stream().anyMatch(h -> h.getHolidayDate().equals(date))) return List.of();
        if (!leaveRepo.findByVendorIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(vendorId, "APPROVED", date, date).isEmpty()) return List.of();
        int day = date.getDayOfWeek().getValue();
        VmsVendorWorkingHours hours = hoursRepo.findByVendorIdOrderByDayOfWeek(vendorId).stream()
                .filter(h -> h.getDayOfWeek() == day && Boolean.TRUE.equals(h.getIsWorkingDay())).findFirst().orElse(null);
        if (hours == null) return List.of();
        int duration = service.getDurationMinutes() == null || service.getDurationMinutes() <= 0 ? 60 : service.getDurationMinutes();
        List<String> slots = new ArrayList<>();
        for (LocalTime start = hours.getStartTime(); !start.plusMinutes(duration).isAfter(hours.getEndTime()); start = start.plusMinutes(30)) {
            slots.add(start.format(TIME));
        }
        return slots;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFavorites(AppUser user) {
        return favoriteRepo.findByUserIdAndCommunityId(user.getId(), user.getCommunity().getId(), org.springframework.data.domain.PageRequest.of(0, 200))
                .getContent().stream().map(f -> Map.<String, Object>of("id", f.getId(), "vendorId", f.getVendor().getId(),
                        "vendorName", f.getVendor().getBusinessName(), "vendorCategory", f.getVendor().getCategory() == null ? "" : f.getVendor().getCategory().getName(),
                        "vendorRating", f.getVendor().getAverageRating(), "addedAt", f.getCreatedAt().toString())).toList();
    }

    @Transactional
    public Map<String, Object> addFavorite(AppUser user, Long vendorId) {
        VmsVendor vendor = requireVendor(vendorId, user.getCommunity().getId());
        if (!favoriteRepo.existsByVendorIdAndUserId(vendorId, user.getId())) {
            favoriteRepo.save(VmsVendorFavorite.builder().vendor(vendor).user(user).community(user.getCommunity()).build());
        }
        return Map.of("vendorId", vendor.getId(), "favorited", true);
    }

    @Transactional
    public void removeFavorite(AppUser user, Long vendorId) {
        favoriteRepo.findByVendorIdAndUserId(vendorId, user.getId()).ifPresent(favoriteRepo::delete);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkFavorite(AppUser user, Long vendorId) {
        return Map.of("favorited", favoriteRepo.existsByVendorIdAndUserId(vendorId, user.getId()));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyDocuments(AppUser user) {
        VmsVendor vendor = requireMyVendor(user);
        return documentRepo.findByVendorIdAndActive(vendor.getId(), true).stream().map(this::documentMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDocuments(Long vendorId, Long communityId) {
        requireVendor(vendorId, communityId);
        return documentRepo.findByVendorIdAndActive(vendorId, true).stream().map(this::documentMap).toList();
    }

    @Transactional
    public Map<String, Object> uploadDocument(AppUser user, MultipartFile file, String type, String name, String expiry) {
        VmsVendor vendor = requireMyVendor(user);
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("A document file is required");
        if (file.getSize() > MAX_DOC_BYTES) throw new IllegalArgumentException("Documents must be 10 MB or smaller");
        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "document");
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!DOC_EXTENSIONS.contains(extension)) throw new IllegalArgumentException("Allowed document types: PDF, JPG, PNG, DOC, DOCX");
        String key = user.getCommunity().getId() + "/" + UUID.randomUUID() + "." + extension;
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Invalid document path");
        try { Files.createDirectories(target.getParent()); Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException e) { throw new IllegalStateException("Unable to store vendor document", e); }
        VmsVendorDocument saved = documentRepo.save(VmsVendorDocument.builder().vendor(vendor).documentType(type)
                .documentName(name).fileUrl(key).fileSize(file.getSize())
                .mimeType(file.getContentType()).expiryDate(expiry == null || expiry.isBlank() ? null : LocalDate.parse(expiry))
                .status(VmsVendorDocument.DocumentStatus.PENDING).version(1).active(true).build());
        return documentMap(saved);
    }

    @Transactional
    public void deleteDocument(AppUser user, Long id) {
        VmsVendor vendor = requireMyVendor(user);
        VmsVendorDocument doc = documentRepo.findByIdAndVendorId(id, vendor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor document", id));
        doc.setActive(false);
        documentRepo.save(doc);
    }

    @Transactional(readOnly = true)
    public Path documentPath(Long id, Long communityId, Long userId, boolean manager) {
        VmsVendorDocument doc = documentRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vendor document", id));
        if (!Objects.equals(doc.getVendor().getCommunity().getId(), communityId)) throw new ResourceNotFoundException("Vendor document", id);
        if ((doc.getVendor().getUser() == null || !doc.getVendor().getUser().getId().equals(userId)) && !manager) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot access this document");
        }
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path path = root.resolve(doc.getFileUrl()).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) throw new ResourceNotFoundException("Vendor document file", id);
        return path;
    }

    @Transactional
    public Map<String, Object> verifyDocument(AppUser user, Long id) {
        VmsVendorDocument doc = documentRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vendor document", id));
        if (!Objects.equals(doc.getVendor().getCommunity().getId(), user.getCommunity().getId())) throw new ResourceNotFoundException("Vendor document", id);
        doc.setStatus(VmsVendorDocument.DocumentStatus.APPROVED);
        doc.setApprovedBy(user); doc.setApprovedAt(LocalDateTime.now());
        return documentMap(documentRepo.save(doc));
    }

    private Map<String, Object> documentMap(VmsVendorDocument d) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", d.getId()); out.put("vendorId", d.getVendor().getId()); out.put("documentType", d.getDocumentType());
        out.put("documentName", d.getDocumentName()); out.put("fileUrl", "/api/vendor/documents/" + d.getId() + "/file"); out.put("fileSize", d.getFileSize());
        out.put("expiryDate", d.getExpiryDate()); out.put("verified", d.getStatus() == VmsVendorDocument.DocumentStatus.APPROVED);
        out.put("status", d.getStatus().name()); out.put("verifiedAt", d.getApprovedAt()); out.put("uploadedAt", d.getCreatedAt()); out.put("version", d.getVersion());
        return out;
    }
}
