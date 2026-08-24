package com.ino.admin.monitoring;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {
    private final MonitoringSummaryService monitoringSummaryService;

    public MonitoringController(MonitoringSummaryService monitoringSummaryService) {
        this.monitoringSummaryService = monitoringSummaryService;
    }

    @GetMapping("/summary")
    MonitoringSummary getSummary() {
        return monitoringSummaryService.getSummary();
    }
}
