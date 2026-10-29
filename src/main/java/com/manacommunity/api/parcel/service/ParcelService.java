package com.manacommunity.api.parcel.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.parcel.entity.ParcelRecord;
import com.manacommunity.api.parcel.repository.ParcelRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ParcelService {

    private final ParcelRecordRepository repo;
    private final Random random = new Random();

    @Transactional
    public ParcelRecord receiveParcel(Long communityId, String flatNo, String courierName, String trackingNumber, Long guardId) {
        return repo.save(ParcelRecord.builder()
                .communityId(communityId)
                .flatNo(flatNo)
                .courierName(courierName)
                .trackingNumber(trackingNumber)
                .receivedByGuardId(guardId)
                .build());
    }

    @Transactional
    public ParcelRecord generateCollectionOtp(Long parcelId) {
        ParcelRecord parcel = repo.findById(parcelId)
                .orElseThrow(() -> new ResourceNotFoundException("ParcelRecord", parcelId));
        String otp = String.format("%06d", random.nextInt(1_000_000));
        parcel.setCollectionOtp(otp);
        parcel.setOtpExpiresAt(LocalDateTime.now().plusHours(24));
        return repo.save(parcel);
    }

    @Transactional
    public ParcelRecord collectParcel(Long parcelId, String otp) {
        ParcelRecord parcel = repo.findById(parcelId)
                .orElseThrow(() -> new ResourceNotFoundException("ParcelRecord", parcelId));
        if (parcel.getCollectionOtp() == null || !parcel.getCollectionOtp().equals(otp)) {
            throw new InvalidInputException("Invalid OTP");
        }
        if (parcel.getOtpExpiresAt() != null && LocalDateTime.now().isAfter(parcel.getOtpExpiresAt())) {
            throw new InvalidInputException("OTP has expired");
        }
        parcel.setStatus("COLLECTED");
        parcel.setCollectedAt(LocalDateTime.now());
        return repo.save(parcel);
    }

    @Transactional(readOnly = true)
    public List<ParcelRecord> getPendingParcels(Long communityId, String flatNo) {
        if (flatNo != null && !flatNo.isBlank()) {
            return repo.findByCommunityIdAndFlatNoAndStatusOrderByCreatedAtDesc(communityId, flatNo, "AWAITING_PICKUP");
        }
        return repo.findByCommunityIdOrderByCreatedAtDesc(communityId);
    }
}
