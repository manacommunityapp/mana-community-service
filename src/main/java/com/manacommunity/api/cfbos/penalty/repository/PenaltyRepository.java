package com.manacommunity.api.cfbos.penalty.repository;
import com.manacommunity.api.cfbos.penalty.entity.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, Long> { List<Penalty> findByInvoiceId(Long invoiceId); List<Penalty> findByResidentId(Long residentId); }
