package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.dto.ServicePackageRequest;
import com.manacommunity.api.homeservices.dto.ServicePackageResponse;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface ServicePackageService {

    List<ServicePackageResponse> getPackages(AppUser user);

    ServicePackageResponse createPackage(ServicePackageRequest request, AppUser user);

    ServicePackageResponse markPaid(Long packageId);
}
