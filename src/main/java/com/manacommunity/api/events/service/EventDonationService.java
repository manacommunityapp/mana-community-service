package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventDonationRequest;
import com.manacommunity.api.events.dto.EventDonationResponse;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.entity.EventDonation;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventDonationRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventDonationService {

    private final EventDonationRepository donationRepo;
    private final EventCommunityRepository eventRepo;
    private final PlatformTransactionManager txManager;

    @Transactional(readOnly = true)
    public List<EventDonationResponse> getByEvent(Long eventId) {
        return donationRepo.findByEventIdOrderByCreatedAtDesc(eventId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventDonationResponse> getByCommunity(Long communityId) {
        return donationRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventDonationResponse create(EventDonationRequest req, AppUser user, Community community) {
        EventCommunity event = eventRepo.findById(req.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", req.getEventId()));

        EventDonation donation = EventDonation.builder()
                .event(event)
                .donorName(req.getDonorName())
                .donorEmail(req.getDonorEmail())
                .donorPhone(req.getDonorPhone())
                .flatNumber(req.getFlatNumber())
                .amount(req.getAmount())
                .paymentMethod(parseEnumOrDefault(EventDonation.PaymentMethod.class, req.getPaymentMethod(), EventDonation.PaymentMethod.CASH))
                .transactionRef(req.getTransactionRef())
                .note(req.getNote())
                .anonymous(req.isAnonymous())
                .community(community)
                .recordedBy(user)
                .build();
        return toResponse(donationRepo.save(donation));
    }

    @Transactional
    public EventDonationResponse update(Long id, EventDonationRequest req) {
        EventDonation d = donationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation", id));
        d.setDonorName(req.getDonorName());
        d.setDonorEmail(req.getDonorEmail());
        d.setDonorPhone(req.getDonorPhone());
        d.setFlatNumber(req.getFlatNumber());
        d.setAmount(req.getAmount());
        d.setPaymentMethod(parseEnumOrDefault(EventDonation.PaymentMethod.class, req.getPaymentMethod(), d.getPaymentMethod()));
        d.setTransactionRef(req.getTransactionRef());
        d.setNote(req.getNote());
        d.setAnonymous(req.isAnonymous());
        return toResponse(donationRepo.save(d));
    }

    @Transactional
    public void delete(Long id) {
        donationRepo.deleteById(id);
    }

    private EventDonationResponse toResponse(EventDonation d) {
        return EventDonationResponse.builder()
                .id(d.getId())
                .eventId(d.getEvent().getId())
                .eventTitle(d.getEvent().getTitle())
                .donorName(d.getDonorName())
                .donorEmail(d.getDonorEmail())
                .donorPhone(d.getDonorPhone())
                .flatNumber(d.getFlatNumber())
                .amount(d.getAmount())
                .paymentMethod(d.getPaymentMethod().name())
                .transactionRef(d.getTransactionRef())
                .note(d.getNote())
                .anonymous(d.isAnonymous())
                .recordedByName(d.getRecordedBy().getFullName())
                .createdAt(d.getCreatedAt() != null ? d.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }

    // ── Template columns ────────────────────────────────────────────────────────
    private static final String[] TEMPLATE_HEADERS = {
        "Event ID*", "Donor Name*", "Donor Email", "Phone Number*", "Flat Number*",
        "Amount*", "Payment Method", "Transaction Reference", "Note", "Anonymous"
    };

    public byte[] generateUploadTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Donations");
            Row header = sheet.createRow(0);

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(TEMPLATE_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Sample row: Event ID, Donor Name, Email, Phone*, Flat Number*, Amount*, Method, TxnRef, Note, Anonymous
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue(1);
            sample.createCell(1).setCellValue("Ramesh Kumar");
            sample.createCell(2).setCellValue("ramesh@example.com");
            sample.createCell(3).setCellValue("+91 9876543210");
            sample.createCell(4).setCellValue("A-101");
            sample.createCell(5).setCellValue(5000);
            sample.createCell(6).setCellValue("CASH");
            sample.createCell(7).setCellValue("");
            sample.createCell(8).setCellValue("Prasadam donation");
            sample.createCell(9).setCellValue("FALSE");

            // Notes sheet
            Sheet notes = wb.createSheet("Notes");
            String[] noteLines = {
                "Payment Method values: CASH, UPI, CHEQUE, BANK_TRANSFER, ONLINE",
                "Anonymous: TRUE or FALSE",
                "Fields marked with * are required",
                "Event ID must match an existing event in the system",
                "Phone Number and Flat Number are mandatory for all rows"
            };
            for (int i = 0; i < noteLines.length; i++) {
                notes.createRow(i).createCell(0).setCellValue(noteLines[i]);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    public BulkUploadOutcome bulkUpload(MultipartFile file, AppUser user, Community community) throws IOException {
        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            if (!rows.hasNext()) return new BulkUploadOutcome(0, 0, 0, new byte[0]);

            // read header row to determine output column positions
            Row headerRow = rows.next();
            int colCount = TEMPLATE_HEADERS.length;
            int statusColIdx = colCount;
            int errorColIdx  = colCount + 1;

            // ensure header row has Status and Error columns
            Cell statusHeaderCell = headerRow.getCell(statusColIdx);
            if (statusHeaderCell == null) statusHeaderCell = headerRow.createCell(statusColIdx);
            statusHeaderCell.setCellValue("Status");
            Cell errorHeaderCell = headerRow.getCell(errorColIdx);
            if (errorHeaderCell == null) errorHeaderCell = headerRow.createCell(errorColIdx);
            errorHeaderCell.setCellValue("Error");

            CellStyle successStyle = wb.createCellStyle();
            successStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            successStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle failStyle = wb.createCellStyle();
            failStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            failStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            int saved = 0, failed = 0, total = 0;

            while (rows.hasNext()) {
                Row row = rows.next();
                if (isRowEmpty(row)) continue;
                total++;

                Cell statusCell = row.getCell(statusColIdx);
                if (statusCell == null) statusCell = row.createCell(statusColIdx);
                Cell errorCell = row.getCell(errorColIdx);
                if (errorCell == null) errorCell = row.createCell(errorColIdx);

                try {
                    TransactionTemplate tx = new TransactionTemplate(txManager);
                    String error = tx.execute(status -> {
                        try { return validateAndSaveRow(row, user, community); }
                        catch (RuntimeException ex) { status.setRollbackOnly(); throw ex; }
                    });
                    if (error == null) {
                        statusCell.setCellValue("Saved");
                        statusCell.setCellStyle(successStyle);
                        errorCell.setCellValue("");
                        saved++;
                    } else {
                        statusCell.setCellValue("Failed");
                        statusCell.setCellStyle(failStyle);
                        errorCell.setCellValue(error);
                        failed++;
                    }
                } catch (Exception ex) {
                    statusCell.setCellValue("Failed");
                    statusCell.setCellStyle(failStyle);
                    errorCell.setCellValue(ex.getMessage() != null ? ex.getMessage() : "Unexpected error");
                    failed++;
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new BulkUploadOutcome(total, saved, failed, out.toByteArray());
        }
    }

    private String validateAndSaveRow(Row row, AppUser user, Community community) {
        // Column order: Event ID*, Donor Name*, Donor Email, Phone Number*, Flat Number*,
        //               Amount*, Payment Method, Transaction Reference, Note, Anonymous
        List<String> errors = new ArrayList<>();

        Long eventId     = parseLong(row, 0);
        String donorName = parseString(row, 1);
        String email     = parseString(row, 2);
        String phone     = parseString(row, 3);
        String flatNo    = parseString(row, 4);
        Double amount    = parseDouble(row, 5);
        String method    = parseString(row, 6);
        String txnRef    = parseString(row, 7);
        String note      = parseString(row, 8);
        boolean anon     = parseBoolean(row, 9);

        if (eventId == null) errors.add("Event ID is required");
        if (!anon && (donorName == null || donorName.isBlank())) errors.add("Donor Name is required");
        if (phone == null || phone.isBlank()) errors.add("Phone Number is required");
        if (flatNo == null || flatNo.isBlank()) errors.add("Flat Number is required");
        if (amount == null || amount <= 0) errors.add("Amount must be > 0");

        if (!errors.isEmpty()) return String.join("; ", errors);

        EventCommunity event = eventRepo.findById(eventId).orElse(null);
        if (event == null) return "Event ID " + eventId + " not found";

        EventDonation donation = EventDonation.builder()
                .event(event)
                .donorName(anon ? "Anonymous" : donorName)
                .donorEmail(email)
                .donorPhone(phone)
                .flatNumber(flatNo)
                .amount(amount)
                .paymentMethod(parseEnumOrDefault(EventDonation.PaymentMethod.class, method, EventDonation.PaymentMethod.CASH))
                .transactionRef(txnRef)
                .note(note)
                .anonymous(anon)
                .community(community)
                .recordedBy(user)
                .build();
        donationRepo.save(donation);
        return null;
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
            Cell c = row.getCell(i);
            if (c != null && c.getCellType() != CellType.BLANK) {
                String v = getCellStringValue(c).trim();
                if (!v.isEmpty()) return false;
            }
        }
        return true;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default      -> "";
        };
    }

    private Long parseLong(Row row, int col) {
        String v = getCellStringValue(row.getCell(col)).trim();
        if (v.isEmpty()) return null;
        try { return Long.parseLong(v.replace(".0", "")); }
        catch (NumberFormatException e) { return null; }
    }

    private Double parseDouble(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        String v = getCellStringValue(cell).trim();
        if (v.isEmpty()) return null;
        try { return Double.parseDouble(v); }
        catch (NumberFormatException e) { return null; }
    }

    private String parseString(Row row, int col) {
        String v = getCellStringValue(row.getCell(col)).trim();
        return v.isEmpty() ? null : v;
    }

    private boolean parseBoolean(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return false;
        if (cell.getCellType() == CellType.BOOLEAN) return cell.getBooleanCellValue();
        String v = getCellStringValue(cell).trim();
        return "TRUE".equalsIgnoreCase(v) || "1".equals(v) || "YES".equalsIgnoreCase(v);
    }

    public record BulkUploadOutcome(int total, int saved, int failed, byte[] resultExcel) {}

    private <E extends Enum<E>> E parseEnumOrDefault(Class<E> enumClass, String value, E def) {
        if (value == null || value.isBlank()) return def;
        try { return Enum.valueOf(enumClass, value.toUpperCase()); }
        catch (IllegalArgumentException e) { return def; }
    }
}
