package com.manacommunity.api.unit.service;

import com.manacommunity.api.fresh.city.dto.CityDto;
import com.manacommunity.api.fresh.city.dto.ServiceZoneDto;
import com.manacommunity.api.fresh.city.dto.ServiceabilityResolutionDto;
import com.manacommunity.api.fresh.city.model.FreshCity;
import com.manacommunity.api.fresh.city.model.FreshServiceZone;
import com.manacommunity.api.fresh.city.model.FreshServiceZonePostalCode;
import com.manacommunity.api.fresh.city.repository.FreshCityRepository;
import com.manacommunity.api.fresh.city.repository.FreshServiceZonePostalCodeRepository;
import com.manacommunity.api.fresh.city.repository.FreshServiceZoneRepository;
import com.manacommunity.api.fresh.city.service.FreshCityService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreshCityServiceTest {

    @Mock private FreshCityRepository cityRepository;
    @Mock private FreshServiceZoneRepository zoneRepository;
    @Mock private FreshServiceZonePostalCodeRepository postalCodeRepository;
    @Mock private CommunityRepository communityRepository;

    @InjectMocks
    private FreshCityService cityService;

    private FreshCity hydCity;
    private FreshServiceZone nzpZone;

    @BeforeEach
    void setUp() {
        hydCity = FreshCity.builder()
                .id(1L)
                .code("HYD")
                .name("Hyderabad")
                .state("Telangana")
                .isActive(true)
                .serviceZones(new ArrayList<>())
                .build();

        nzpZone = FreshServiceZone.builder()
                .id(10L)
                .city(hydCity)
                .name("Nizampet")
                .code("HYD-NZP")
                .isActive(true)
                .deliveryCutoffTime(LocalTime.of(22, 0))
                .postalCodes(new ArrayList<>())
                .build();

        hydCity.getServiceZones().add(nzpZone);
    }

    @Test
    void testGetAllActiveCities_ReturnsMappedCities() {
        when(cityRepository.findByIsActiveTrue()).thenReturn(List.of(hydCity));

        List<CityDto> cities = cityService.getAllActiveCities();

        assertNotNull(cities);
        assertEquals(1, cities.size());
        assertEquals("HYD", cities.get(0).getCode());
        assertEquals(1, cities.get(0).getServiceZones().size());
        assertEquals("Nizampet", cities.get(0).getServiceZones().get(0).getName());
    }

    @Test
    void testResolveServiceability_ByCommunity() {
        Community comm = Community.builder()
                .id(100L)
                .name("Prestige Oasis")
                .serviceZone(nzpZone)
                .build();

        when(communityRepository.findById(100L)).thenReturn(Optional.of(comm));

        ServiceabilityResolutionDto res = cityService.resolveServiceability(null, 100L);

        assertNotNull(res);
        assertTrue(res.isServiceable());
        assertEquals("Hyderabad", res.getCityName());
        assertEquals("Nizampet", res.getZoneName());
    }

    @Test
    void testResolveServiceability_ByPostalCode() {
        FreshServiceZonePostalCode zp = FreshServiceZonePostalCode.builder()
                .id(1L)
                .zone(nzpZone)
                .postalCode("500090")
                .locality("Nizampet Village")
                .build();

        when(postalCodeRepository.findByPostalCode("500090")).thenReturn(List.of(zp));

        ServiceabilityResolutionDto res = cityService.resolveServiceability("500090", null);

        assertNotNull(res);
        assertTrue(res.isServiceable());
        assertEquals("Hyderabad", res.getCityName());
        assertEquals("500090", res.getPostalCode());
    }

    @Test
    void testResolveServiceability_OutsideCoverage() {
        when(postalCodeRepository.findByPostalCode("999999")).thenReturn(List.of());

        ServiceabilityResolutionDto res = cityService.resolveServiceability("999999", null);

        assertNotNull(res);
        assertFalse(res.isServiceable());
    }
}
