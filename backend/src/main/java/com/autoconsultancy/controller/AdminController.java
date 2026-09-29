package com.autoconsultancy.controller;

import com.autoconsultancy.dto.request.CreateWorkerRequest;
import com.autoconsultancy.dto.response.ApiResponse;
import com.autoconsultancy.dto.response.DashboardStatsResponse;
import com.autoconsultancy.dto.response.PageResponse;
import com.autoconsultancy.entity.AuditLog;
import com.autoconsultancy.entity.Customer;
import com.autoconsultancy.entity.Worker;
import com.autoconsultancy.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats(), "Dashboard fetched"));
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<PageResponse<Customer>>> getCustomers(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(adminService.getCustomers(PageRequest.of(page, size))), "Customers fetched"));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<ApiResponse<Customer>> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getCustomer(id), "Customer fetched"));
    }

    @PutMapping("/customers/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateCustomerStatus(@PathVariable Long id, @RequestParam boolean active) {
        adminService.updateCustomerStatus(id, active);
        return ResponseEntity.ok(ApiResponse.success(null, "Customer status updated"));
    }

    @GetMapping("/workers")
    public ResponseEntity<ApiResponse<PageResponse<Worker>>> getWorkers(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(adminService.getWorkers(PageRequest.of(page, size))), "Workers fetched"));
    }

    @PostMapping("/workers")
    public ResponseEntity<ApiResponse<Worker>> createWorker(@RequestBody @jakarta.validation.Valid CreateWorkerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.createWorker(request), "Worker created"));
    }

    @GetMapping("/workers/{id}")
    public ResponseEntity<ApiResponse<Worker>> getWorker(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getWorker(id), "Worker fetched"));
    }

    @PutMapping("/workers/{id}")
    public ResponseEntity<ApiResponse<Worker>> updateWorker(@PathVariable Long id, @RequestBody @jakarta.validation.Valid CreateWorkerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateWorker(id, request), "Worker updated"));
    }

    @DeleteMapping("/workers/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateWorker(@PathVariable Long id) {
        adminService.deactivateWorker(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Worker deactivated"));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> getAuditLogs(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(adminService.getAuditLogs(PageRequest.of(page, size))), "Audit logs fetched"));
    }
}
