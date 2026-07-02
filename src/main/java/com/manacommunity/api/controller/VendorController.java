package com.manacommunity.api.controller;

import com.manacommunity.api.model.Vendor;
import com.manacommunity.api.repository.VendorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asset-finance/vendors")
public class VendorController {

    private final VendorRepository vendorRepository;

    public VendorController(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @GetMapping
    public ResponseEntity<List<Vendor>> getVendors(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(activeOnly
                ? vendorRepository.findByActiveTrueOrderByNameAsc()
                : vendorRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vendor> getVendor(@PathVariable Long id) {
        return vendorRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Vendor> createVendor(@RequestBody Vendor vendor) {
        return ResponseEntity.ok(vendorRepository.save(vendor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vendor> updateVendor(@PathVariable Long id, @RequestBody Vendor request) {
        return vendorRepository.findById(id)
                .map(vendor -> {
                    request.setId(vendor.getId());
                    request.setCreatedAt(vendor.getCreatedAt());
                    return ResponseEntity.ok(vendorRepository.save(request));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
