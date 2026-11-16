package com.manacommunity.api.vendor.master.service;

import com.manacommunity.api.vendor.master.dto.CreateManaVendorRequest;
import com.manacommunity.api.vendor.master.dto.ManaVendorDto;
import com.manacommunity.api.vendor.master.dto.UpdateManaVendorRequest;
import com.manacommunity.api.vendor.master.entity.ManaVendor;
import com.manacommunity.api.vendor.master.repository.ManaVendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManaVendorService {

    private final ManaVendorRepository repository;

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ManaVendorDto> listByCommunity(Long communityId) {
        return repository.findByCommunityIdOrderByNameAsc(communityId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ManaVendorDto> listActiveByCommunity(Long communityId) {
        return repository.findByCommunityIdAndStatusOrderByNameAsc(communityId, "ACTIVE")
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ManaVendorDto> pageByCommunity(Long communityId, int page, int size) {
        return repository.findByCommunityId(
                communityId,
                PageRequest.of(page, size, Sort.by("name").ascending())
        ).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ManaVendorDto> search(Long communityId, String q) {
        if (q == null || q.isBlank()) return listActiveByCommunity(communityId);
        return repository.searchActive(communityId, q.trim())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ManaVendorDto getById(Long communityId, Long id) {
        return repository.findByIdAndCommunityId(id, communityId)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + id));
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @Transactional
    public ManaVendorDto create(CreateManaVendorRequest req) {
        if (req.getGstin() != null && !req.getGstin().isBlank()
                && repository.existsByGstinAndCommunityId(req.getGstin(), req.getCommunityId())) {
            throw new IllegalArgumentException(
                    "A vendor with GSTIN " + req.getGstin() + " already exists in this community");
        }

        ManaVendor vendor = ManaVendor.builder()
                .communityId(req.getCommunityId())
                .name(req.getName().strip())
                .contactPerson(req.getContactPerson())
                .email(req.getEmail())
                .phone(req.getPhone())
                .gstin(req.getGstin())
                .pan(req.getPan())
                .address(req.getAddress())
                .city(req.getCity())
                .state(req.getState())
                .pincode(req.getPincode())
                .bankDetails(req.getBankDetails())
                .categories(req.getCategories())
                .rating(req.getRating())
                .sourceModule(req.getSourceModule() != null ? req.getSourceModule() : "MANUAL")
                .status("ACTIVE")
                .build();

        return toDto(repository.save(vendor));
    }

    @Transactional
    public ManaVendorDto update(Long communityId, Long id, UpdateManaVendorRequest req) {
        ManaVendor vendor = repository.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + id));

        if (req.getGstin() != null && !req.getGstin().isBlank()
                && !req.getGstin().equals(vendor.getGstin())
                && repository.existsByGstinAndCommunityIdAndIdNot(req.getGstin(), communityId, id)) {
            throw new IllegalArgumentException(
                    "A vendor with GSTIN " + req.getGstin() + " already exists in this community");
        }

        if (req.getName() != null && !req.getName().isBlank()) vendor.setName(req.getName().strip());
        if (req.getContactPerson() != null) vendor.setContactPerson(req.getContactPerson());
        if (req.getEmail() != null)         vendor.setEmail(req.getEmail());
        if (req.getPhone() != null)         vendor.setPhone(req.getPhone());
        if (req.getGstin() != null)         vendor.setGstin(req.getGstin());
        if (req.getPan() != null)           vendor.setPan(req.getPan());
        if (req.getAddress() != null)       vendor.setAddress(req.getAddress());
        if (req.getCity() != null)          vendor.setCity(req.getCity());
        if (req.getState() != null)         vendor.setState(req.getState());
        if (req.getPincode() != null)       vendor.setPincode(req.getPincode());
        if (req.getBankDetails() != null)   vendor.setBankDetails(req.getBankDetails());
        if (req.getCategories() != null)    vendor.setCategories(req.getCategories());
        if (req.getRating() != null)        vendor.setRating(req.getRating());
        if (req.getStatus() != null)        vendor.setStatus(req.getStatus().toUpperCase());

        return toDto(repository.save(vendor));
    }

    @Transactional
    public void deactivate(Long communityId, Long id) {
        ManaVendor vendor = repository.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + id));
        vendor.setStatus("INACTIVE");
        repository.save(vendor);
        log.info("Vendor {} deactivated in community {}", id, communityId);
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private ManaVendorDto toDto(ManaVendor v) {
        return ManaVendorDto.builder()
                .id(v.getId())
                .communityId(v.getCommunityId())
                .name(v.getName())
                .contactPerson(v.getContactPerson())
                .email(v.getEmail())
                .phone(v.getPhone())
                .gstin(v.getGstin())
                .pan(v.getPan())
                .address(v.getAddress())
                .city(v.getCity())
                .state(v.getState())
                .pincode(v.getPincode())
                .bankDetails(v.getBankDetails())
                .categories(v.getCategories())
                .rating(v.getRating())
                .sourceModule(v.getSourceModule())
                .status(v.getStatus())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}
