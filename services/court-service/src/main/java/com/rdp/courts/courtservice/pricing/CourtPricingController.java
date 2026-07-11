package com.rdp.courts.courtservice.pricing;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courts")
class CourtPricingController {
    private final CourtPricingService courtPricingService;

    CourtPricingController(CourtPricingService courtPricingService) {
        this.courtPricingService = courtPricingService;
    }

    @GetMapping("/pricing")
    ResponseEntity<CourtPricingResponse> getCourtPricing(@RequestParam LocalDate date, @RequestParam LocalTime time) {
        return courtPricingService.calculateFee(date, time).map(fee -> new CourtPricingResponse(fee))
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
