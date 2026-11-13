package com.shakil.pulsequeue.controller;

import com.shakil.pulsequeue.dto.QueueStatsResponse;
import com.shakil.pulsequeue.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics & Live Stats", description = "Real-time clinic throughput and operational metrics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/live")
    @Operation(summary = "Get live throughput stats, wait estimates, and active department loads")
    public ResponseEntity<QueueStatsResponse> getLiveStats() {
        return ResponseEntity.ok(analyticsService.getLiveStats());
    }
}
