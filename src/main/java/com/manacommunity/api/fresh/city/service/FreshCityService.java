package com.manacommunity.api.fresh.city.service;

import com.manacommunity.api.fresh.city.dto.CityDto;
import com.manacommunity.api.fresh.city.dto.ServiceZoneDto;
import com.manacommunity.api.fresh.city.dto.ServiceabilityResolutionDto;
import com.manacommunity.api.fresh.city.model.FreshCity;
import com.manacommunity.api.fresh.city.model.FreshServiceZone;
import com.manacommunity.api.fresh.city.model.FreshServiceZonePostalCode;
import com.manacommunity.api.fresh.city.repository.FreshCityRepository;
import com.manacommunity.api.fresh.city.repository.FreshServiceZonePostalCodeRepository;
import com.manacommunity.api.fresh.city.repository.FreshServiceZoneRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreshCityService {

    private final FreshCityRepository cityRepository;
    private final FreshServiceZoneRepository zoneRepository;
    private final FreshServiceZonePostalCodeRepository postalCodeRepository;
    private final CommunityRepository communityRepository;

    @Transactional(readOnly = true)
    public List<CityDto> getAllActiveCities() {
        return cityRepository.findByIsActiveTrue().stream()
                .map(this::mapCityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceZoneDto> getServiceZonesByCity(Long cityId) {
        return zoneRepository.findByCityIdAndIsActiveTrue(cityId).stream()
                .map(this::mapZoneToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceabilityResolutionDto resolveServiceability(String postalCode, Long communityId) {
        // 1. Resolve via Community if provided
        if (communityId != null) {
            Optional<Community> commOpt = communityRepository.findById(communityId);
            if (commOpt.isPresent()) {
                Community c = commOpt.get();
                if (c.getServiceZone() != null && Boolean.TRUE.equals(c.getServiceZone().getIsActive())) {
                    FreshServiceZone z = c.getServiceZone();
                    FreshCity city = z.getCity();
                    return ServiceabilityResolutionDto.builder()
                            .isServiceable(true)
                            .cityId(city.getId())
                            .cityCode(city.getCode())
                            .cityName(city.getName())
                            .serviceZoneId(z.getId())
                            .zoneCode(z.getCode())
                            .zoneName(z.getName())
                            .postalCode(postalCode)
                            .message("Community '" + c.getName() + "' is fully serviceable in zone " + z.getName())
                            .build();
                }
            }
        }

        // 2. Resolve via Postal Code
        if (postalCode != null && !postalCode.isBlank()) {
            List<FreshServiceZonePostalCode> zonePostals = postalCodeRepository.findByPostalCode(postalCode.trim());
            for (FreshServiceZonePostalCode zp : zonePostals) {
                FreshServiceZone z = zp.getZone();
                if (z != null && Boolean.TRUE.equals(z.getIsActive())) {
                    FreshCity city = z.getCity();
                    return ServiceabilityResolutionDto.builder()
                            .isServiceable(true)
                            .cityId(city.getId())
                            .cityCode(city.getCode())
                            .cityName(city.getName())
                            .serviceZoneId(z.getId())
                            .zoneCode(z.getCode())
                            .zoneName(z.getName())
                            .postalCode(postalCode)
                            .message("Postal code " + postalCode + " is serviceable in " + city.getName() + " (" + z.getName() + ")")
                            .build();
                }
            }
        }

        return ServiceabilityResolutionDto.builder()
                .isServiceable(false)
                .postalCode(postalCode)
                .message("Location is currently outside active service zones. Expanding soon!")
                .build();
    }

    private CityDto mapCityToDto(FreshCity city) {
        List<ServiceZoneDto> zones = city.getServiceZones().stream()
                .filter(z -> Boolean.TRUE.equals(z.getIsActive()))
                .map(this::mapZoneToDto)
                .collect(Collectors.toList());

        return CityDto.builder()
                .id(city.getId())
                .code(city.getCode())
                .name(city.getName())
                .state(city.getState())
                .country(city.getCountry())
                .timezone(city.getTimezone())
                .isActive(city.getIsActive())
                .serviceZones(zones)
                .build();
    }

    private ServiceZoneDto mapZoneToDto(FreshServiceZone zone) {
        List<String> postals = zone.getPostalCodes().stream()
                .map(FreshServiceZonePostalCode::getPostalCode)
                .collect(Collectors.toList());

        return ServiceZoneDto.builder()
                .id(zone.getId())
                .cityId(zone.getCity() != null ? zone.getCity().getId() : null)
                .cityName(zone.getCity() != null ? zone.getCity().getName() : null)
                .name(zone.getName())
                .code(zone.getCode())
                .isActive(zone.getIsActive())
                .deliveryCutoffTime(zone.getDeliveryCutoffTime())
                .postalCodes(postals)
                .build();
    }
}
