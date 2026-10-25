package com.manacommunity.api.repository;

import com.manacommunity.api.model.VehicleBookingAddon;
import com.manacommunity.api.model.VehicleCareBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleBookingAddonRepository extends JpaRepository<VehicleBookingAddon, Long> {

    List<VehicleBookingAddon> findByBooking(VehicleCareBooking booking);
}
