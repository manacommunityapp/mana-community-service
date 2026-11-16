package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HrOfferRepository extends JpaRepository<HrOffer, Long> {
    List<HrOffer> findByApplicantId(Long applicantId);
}
