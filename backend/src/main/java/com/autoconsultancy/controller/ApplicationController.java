package com.autoconsultancy.controller;

import com.autoconsultancy.dto.request.ApplicationRequest;
import com.autoconsultancy.dto.request.BikeDetailRequest;
import com.autoconsultancy.dto.request.FinanceDetailRequest;
import com.autoconsultancy.dto.request.StatusUpdateRequest;
import com.autoconsultancy.dto.request.WorkerAssignmentRequest;
import com.autoconsultancy.dto.response.ApiResponse;
import com.autoconsultancy.dto.response.ApplicationDetailResponse;
import com.autoconsultancy.dto.response.ApplicationResponse;
import com.autoconsultancy.dto.response.PageResponse;
import com.autoconsultancy.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> createApplication(
            @RequestBody(required = false) ApplicationRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.create(authentication.getName(), request), "Created draft application"));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getMyApplications(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getMyApplications(authentication.getName()), "Fetched own applications"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'WORKER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationDetailResponse>> getApplicationDetail(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getApplicationDetail(id, authentication.getName()), "Fetched application detail"));
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> submitApplication(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.submit(id, authentication.getName()), "Submitted application"));
    }

    @PutMapping("/{id}/bike-details")
    @PreAuthorize("hasAnyAuthority('WORKER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateBikeDetails(@PathVariable Long id, @RequestBody BikeDetailRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.updateBikeDetails(id, request, authentication.getName()), "Updated bike details"));
    }

    @PutMapping("/{id}/finance-details")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'WORKER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateFinanceDetails(@PathVariable Long id, @RequestBody FinanceDetailRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.updateFinanceDetails(id, request, authentication.getName()), "Updated finance details"));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.updateStatus(id, request, authentication.getName()), "Updated status"));
    }

    @PutMapping("/{id}/assign-worker")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> assignWorker(@PathVariable Long id, @RequestBody WorkerAssignmentRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.assignWorker(id, request, authentication.getName()), "Worker assigned"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ApplicationResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        PageResponse<ApplicationResponse> pageResponse = PageResponse.of(
                applicationService.getAll(PageRequest.of(page, size), status, search));
        return ResponseEntity.ok(ApiResponse.success(pageResponse, "Fetched all applications"));
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasAuthority('WORKER')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getAssignedApplications(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getAssignedApplications(authentication.getName()), "Fetched assigned applications"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Application deleted successfully"));
    }
}
