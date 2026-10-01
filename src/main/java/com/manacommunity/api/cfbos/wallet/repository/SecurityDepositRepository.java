package com.manacommunity.api.cfbos.wallet.repository;
import com.manacommunity.api.cfbos.wallet.entity.SecurityDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface SecurityDepositRepository extends JpaRepository<SecurityDeposit, Long> { List<SecurityDeposit> findByResidentId(Long residentId); List<SecurityDeposit> findByPropertyId(Long propertyId); }
