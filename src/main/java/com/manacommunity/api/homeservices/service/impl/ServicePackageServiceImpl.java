package com.manacommunity.api.homeservices.service.impl;

import com.manacommunity.api.homeservices.dto.ServicePackageRequest;
import com.manacommunity.api.homeservices.dto.ServicePackageResponse;
import com.manacommunity.api.homeservices.model.DomesticStaff;
import com.manacommunity.api.homeservices.model.ServicePackage;
import com.manacommunity.api.homeservices.repository.DomesticStaffRepository;
import com.manacommunity.api.homeservices.repository.ServicePackageRepository;
import com.manacommunity.api.homeservices.service.ServicePackageService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicePackageServiceImpl implements ServicePackageService {

    private final ServicePackageRepository packageRepository;
    private final DomesticStaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ServicePackageResponse> getPackages(AppUser user) {
        return packageRepository.findByResidentIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ServicePackageResponse createPackage(ServicePackageRequest request, AppUser user) {
        DomesticStaff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Staff not found with id: " + request.getStaffId()));

        ServicePackage pkg = ServicePackage.builder()
                .staff(staff)
                .resident(user)
                .flatNumber(request.getFlatNumber())
                .services(request.getServices() != null ? request.getServices() : List.of())
                .monthlySalary(request.getMonthlySalary())
                .nextDueDate(request.getNextDueDate())
                .build();

        return toResponse(packageRepository.save(pkg));
    }

    @Override
    @Transactional
    public ServicePackageResponse markPaid(Long packageId) {
        ServicePackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Package not found with id: " + packageId));

        pkg.setPaymentStatus(ServicePackage.PaymentStatus.PAID);
        pkg.setLastPaidDate(LocalDate.now());
        if (pkg.getNextDueDate() != null) {
            pkg.setNextDueDate(pkg.getNextDueDate().plusMonths(1));
        }

        return toResponse(packageRepository.save(pkg));
    }

    private ServicePackageResponse toResponse(ServicePackage pkg) {
        ServicePackageResponse response = new ServicePackageResponse();
        response.setId(pkg.getId());
        response.setStaffId(pkg.getStaff().getId());
        response.setStaffName(pkg.getStaff().getName());
        response.setResidentId(pkg.getResident().getId());
        response.setResidentName(pkg.getResident().getFullName());
        response.setFlatNumber(pkg.getFlatNumber());
        response.setServices(pkg.getServices());
        response.setMonthlySalary(pkg.getMonthlySalary());
        response.setPaymentStatus(pkg.getPaymentStatus().name());
        response.setLastPaidDate(pkg.getLastPaidDate());
        response.setNextDueDate(pkg.getNextDueDate());
        response.setCreatedAt(pkg.getCreatedAt());
        return response;
    }
}
