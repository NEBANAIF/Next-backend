package com.ousman.controller;

import com.ousman.dto.analytics.AnalyticsDashboardResponse;
import com.ousman.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    /**
     * Database-backed KPIs and time-series for dashboards.
     *
     * @param from        inclusive (yyyy-MM-dd)
     * @param to          inclusive (yyyy-MM-dd)
     * @param granularity day | month | hour (hour only when from = to)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "day") String granularity,
        @RequestParam(defaultValue = "true") boolean includeSeries,
        @RequestParam(required = false) Long branchId
    ) {
        try {
            String g = granularity == null ? "day" : granularity;
            if ("hour".equalsIgnoreCase(g) && !from.equals(to)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "hour granularity requires from and to to be the same day"));
            }
            AnalyticsDashboardResponse body = analyticsService.dashboard(from, to, g, includeSeries, branchId);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
