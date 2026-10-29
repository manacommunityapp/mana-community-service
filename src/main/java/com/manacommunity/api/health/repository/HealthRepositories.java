package com.manacommunity.api.health.repository;

public interface HealthRepositories {
    interface HealthProviderRepository extends com.manacommunity.api.health.repository.HealthProviderRepository {}
    interface HealthSpecialtyRepository extends com.manacommunity.api.health.repository.HealthSpecialtyRepository {}
    interface HealthDoctorRepository extends com.manacommunity.api.health.repository.HealthDoctorRepository {}
    interface HealthQualificationRepository extends com.manacommunity.api.health.repository.HealthQualificationRepository {}
    interface HealthRegistrationRepository extends com.manacommunity.api.health.repository.HealthRegistrationRepository {}
    interface HealthClinicRepository extends com.manacommunity.api.health.repository.HealthClinicRepository {}
    interface HealthAvailabilityRepository extends com.manacommunity.api.health.repository.HealthAvailabilityRepository {}
    interface HealthAppointmentRepository extends com.manacommunity.api.health.repository.HealthAppointmentRepository {}
    interface HealthPatientRepository extends com.manacommunity.api.health.repository.HealthPatientRepository {}
    interface HealthPatientConsentRepository extends com.manacommunity.api.health.repository.HealthPatientConsentRepository {}
    interface HealthMedicalRecordRepository extends com.manacommunity.api.health.repository.HealthMedicalRecordRepository {}
    interface HealthCampRepository extends com.manacommunity.api.health.repository.HealthCampRepository {}
    interface HealthCampRegistrationRepository extends com.manacommunity.api.health.repository.HealthCampRegistrationRepository {}
    interface HealthBenefitUsageRepository extends com.manacommunity.api.health.repository.HealthBenefitUsageRepository {}
}