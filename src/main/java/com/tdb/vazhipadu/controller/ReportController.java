package com.tdb.vazhipadu.controller;

import com.tdb.vazhipadu.dto.response.DailyCollectionReport;
import com.tdb.vazhipadu.service.ReportService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/temples/{templeId}/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/daily-collection")
    public DailyCollectionReport dailyCollection(
            @PathVariable Long templeId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reportService.dailyCollection(templeId, date);
    }
}
