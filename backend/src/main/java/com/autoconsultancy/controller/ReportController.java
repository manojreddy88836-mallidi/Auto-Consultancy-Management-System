package com.autoconsultancy.controller;

import com.autoconsultancy.dto.response.ApiResponse;
import com.autoconsultancy.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/applications-by-month")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getApplicationsByMonth() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getApplicationsByMonth(), "Fetched monthly applications"));
    }

    @GetMapping("/applications-by-status")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getApplicationsByStatus() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getApplicationsByStatus(), "Fetched applications by status"));
    }

    @GetMapping("/finance-stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getFinanceStats() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getFinanceStats(), "Fetched finance stats"));
    }

    @GetMapping("/manufacturer-stats")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getManufacturerStats() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getManufacturerStats(), "Fetched manufacturer stats"));
    }

    @GetMapping("/applications-by-worker")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getApplicationsByWorker() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getApplicationsByWorker(), "Fetched worker applications stats"));
    }
}
