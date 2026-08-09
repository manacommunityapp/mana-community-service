package com.manacommunity.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.dto.events.EventProspectusDto;
import com.manacommunity.api.model.EventProspectusConfig;
import com.manacommunity.api.repository.EventProspectusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProspectusService {

    private final EventProspectusRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public EventProspectusDto getProspectus(Long eventId) {
        EventProspectusConfig config = null;
        if (eventId != null) {
            config = repository.findByEventId(eventId).orElse(null);
        }
        if (config == null) {
            config = repository.findFirstByOrderByUpdatedAtDesc().orElse(null);
        }

        if (config == null) {
            return EventProspectusDto.builder()
                    .bankName("State Bank of India")
                    .accountName("Mana Community Cultural Trust")
                    .accountNumber("394820194829")
                    .ifscCode("SBIN0004829")
                    .upiId("manacommunity@sbi")
                    .qrCodeUrl("https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=400&q=80")
                    .helplines(List.of("+91 98765 43210", "+91 91234 56789"))
                    .contacts(List.of(
                            EventProspectusDto.PaymentContactPersonDto.builder()
                                    .id("c1").name("Rajesh Sharma").role("Treasurer").flatNo("A-402").phone("+91 98765 43210").build(),
                            EventProspectusDto.PaymentContactPersonDto.builder()
                                    .id("c2").name("Suresh Varma").role("President").flatNo("B-105").phone("+91 91234 56789").build()
                    ))
                    .packages(List.of(
                            EventProspectusDto.CorporatePackageDto.builder()
                                    .id("sp1").title("Title Sponsor").amount("₹1,00,000").badge("Exclusive").features(List.of("Main Stage Banner Branding", "VIP Seating (10 Passes)", "Stall Space 10x10", "Logo on All Banners & Passes")).color("from-amber-500 to-yellow-600").totalSlots("1").claimedSlots("0").build(),
                            EventProspectusDto.CorporatePackageDto.builder()
                                    .id("sp2").title("Platinum Sponsor").amount("₹50,000").badge("Popular").features(List.of("Main Stage Side Banner", "VIP Seating (6 Passes)", "Stall Space 8x8", "Logo on Passes")).color("from-slate-400 to-slate-600").totalSlots("3").claimedSlots("1").build(),
                            EventProspectusDto.CorporatePackageDto.builder()
                                    .id("sp3").title("Gold Sponsor").amount("₹25,000").badge("Value").features(List.of("Ground Perimeter Banner", "VIP Seating (4 Passes)", "Logo on Flyer")).color("from-amber-600 to-amber-700").totalSlots("5").claimedSlots("2").build()
                    ))
                    .donations(List.of(
                            EventProspectusDto.DonationSchemeDto.builder()
                                    .id("d1").title("Mahaprasadam Seva").amount("₹5,001").desc("Sponsor lunch & dinner prasadam for 500+ devotees on festival day").badge("80G Tax Exempt").category("Annadhanam").build(),
                            EventProspectusDto.DonationSchemeDto.builder()
                                    .id("d2").title("Pushpalankara Seva").amount("₹3,001").desc("Sponsor grand floral decoration for deity & main stage area").badge("80G Tax Exempt").category("Decoration").build()
                    ))
                    .build();
        }

        return mapToDto(config);
    }

    @Transactional
    public EventProspectusDto saveProspectus(EventProspectusDto dto) {
        EventProspectusConfig config;
        if (dto.getEventId() != null) {
            config = repository.findByEventId(dto.getEventId())
                    .orElseGet(() -> EventProspectusConfig.builder().eventId(dto.getEventId()).build());
        } else if (dto.getId() != null) {
            config = repository.findById(dto.getId())
                    .orElseGet(EventProspectusConfig::new);
        } else {
            config = repository.findFirstByOrderByUpdatedAtDesc()
                    .orElseGet(EventProspectusConfig::new);
        }

        config.setEventTitle(dto.getEventTitle());
        config.setBankName(dto.getBankName());
        config.setAccountName(dto.getAccountName());
        config.setAccountNumber(dto.getAccountNumber());
        config.setIfscCode(dto.getIfscCode());
        config.setUpiId(dto.getUpiId());
        config.setQrCodeUrl(dto.getQrCodeUrl());

        config.setHelplinesJson(toJson(dto.getHelplines()));
        config.setContactsJson(toJson(dto.getContacts()));
        config.setPackagesJson(toJson(dto.getPackages()));
        config.setDonationsJson(toJson(dto.getDonations()));

        EventProspectusConfig saved = repository.save(config);
        log.info("Saved Event Prospectus Config with ID: {}", saved.getId());
        return mapToDto(saved);
    }

    private EventProspectusDto mapToDto(EventProspectusConfig config) {
        return EventProspectusDto.builder()
                .id(config.getId())
                .eventId(config.getEventId())
                .eventTitle(config.getEventTitle())
                .bankName(config.getBankName())
                .accountName(config.getAccountName())
                .accountNumber(config.getAccountNumber())
                .ifscCode(config.getIfscCode())
                .upiId(config.getUpiId())
                .qrCodeUrl(config.getQrCodeUrl())
                .helplines(fromJson(config.getHelplinesJson(), new TypeReference<List<String>>() {}))
                .contacts(fromJson(config.getContactsJson(), new TypeReference<List<EventProspectusDto.PaymentContactPersonDto>>() {}))
                .packages(fromJson(config.getPackagesJson(), new TypeReference<List<EventProspectusDto.CorporatePackageDto>>() {}))
                .donations(fromJson(config.getDonationsJson(), new TypeReference<List<EventProspectusDto.DonationSchemeDto>>() {}))
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON", e);
            return null;
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) return (T) Collections.emptyList();
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.error("Failed to deserialize JSON string", e);
            return (T) Collections.emptyList();
        }
    }
}
