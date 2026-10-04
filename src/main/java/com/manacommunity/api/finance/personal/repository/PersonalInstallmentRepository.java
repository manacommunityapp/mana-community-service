package com.manacommunity.api.finance.personal.repository;

import com.manacommunity.api.finance.personal.entity.PersonalInstallment;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalInstallmentRepository extends JpaRepository<PersonalInstallment, String> {
    List<PersonalInstallment> findByUserOrderByCreatedAtDesc(AppUser user);
    Optional<PersonalInstallment> findByIdAndUser(String id, AppUser user);
}
