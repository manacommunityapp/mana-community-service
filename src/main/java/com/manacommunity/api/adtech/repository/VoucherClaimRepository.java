package com.manacommunity.api.adtech.repository;

import com.manacommunity.api.adtech.entity.VoucherClaimEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoucherClaimRepository extends JpaRepository<VoucherClaimEntity, String> {

    Optional<VoucherClaimEntity> findByCidAndUid(String cid, String uid);

    List<VoucherClaimEntity> findByUidOrderByIssuedAtDesc(String uid);

    long countByVoucherId(String voucherId);
}
