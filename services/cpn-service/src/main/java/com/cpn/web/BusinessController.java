package com.cpn.web;

import com.cpn.application.business.BusinessService;
import com.cpn.domain.business.model.Business;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cpn/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @GetMapping
    public ResponseEntity<List<Business>> getAllBusinesses() {
        return ResponseEntity.ok(businessService.getAllBusinesses());
    }

    @PostMapping
    public ResponseEntity<Business> registerBusiness(@RequestBody Business business) {
        return ResponseEntity.ok(businessService.registerBusiness(business));
    }
}
