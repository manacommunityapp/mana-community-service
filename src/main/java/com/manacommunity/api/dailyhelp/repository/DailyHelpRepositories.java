package com.manacommunity.api.dailyhelp.repository;

import com.manacommunity.api.dailyhelp.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyHelpRepositories {

    interface GlobalStaffProfileRepository extends JpaRepository<GlobalStaffProfile, Long> {
        Optional<GlobalStaffProfile> findByStaffUuid(String staffUuid);
        Optional<GlobalStaffProfile> findByPhoneHash(String phoneHash);
    }

    interface SocietyStaffAffiliationRepository extends JpaRepository<SocietyStaffAffiliation, Long> {
        Optional<SocietyStaffAffiliation> findByCommunityIdAndStaffProfile_Id(Long communityId, Long staffId);
        Optional<SocietyStaffAffiliation> findByCommunityIdAndStaffProfile_StaffUuid(Long communityId, String staffUuid);
        List<SocietyStaffAffiliation> findByStaffProfile_StaffUuid(String staffUuid);
    }

    interface FlatStaffContractRepository extends JpaRepository<FlatStaffContract, Long> {
        List<FlatStaffContract> findByCommunityIdAndFlatIdAndIsActiveTrue(Long communityId, Long flatId);
        List<FlatStaffContract> findByAffiliation_IdAndIsActiveTrue(Long affiliationId);
        List<FlatStaffContract> findByCommunityIdAndAffiliation_StaffProfile_StaffUuidAndIsActiveTrue(
                Long communityId, String staffUuid);
    }

    interface FlatAttendanceSessionRepository extends JpaRepository<FlatAttendanceSession, Long> {
        Optional<FlatAttendanceSession> findByContract_IdAndWorkDate(Long contractId, LocalDate workDate);
        List<FlatAttendanceSession> findByContract_IdAndWorkDateBetween(Long contractId, LocalDate startDate, LocalDate endDate);

        @Query("""
               SELECT s FROM FlatAttendanceSession s
               WHERE s.contract.community.id = :communityId
                 AND s.contract.flatId = :flatId
                 AND s.workDate BETWEEN :startDate AND :endDate
               ORDER BY s.workDate DESC
               """)
        List<FlatAttendanceSession> findFlatAttendanceHistory(
                @Param("communityId") Long communityId,
                @Param("flatId") Long flatId,
                @Param("startDate") LocalDate startDate,
                @Param("endDate") LocalDate endDate);
    }

    interface StaffMonthlySalaryLedgerRepository extends JpaRepository<StaffMonthlySalaryLedger, Long> {
        Optional<StaffMonthlySalaryLedger> findByContract_IdAndBillingMonth(Long contractId, String billingMonth);
        List<StaffMonthlySalaryLedger> findByContract_Community_IdAndContract_FlatIdAndBillingMonth(
                Long communityId, Long flatId, String billingMonth);
    }
}
