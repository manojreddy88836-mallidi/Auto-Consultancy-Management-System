package com.autoconsultancy.controller;

import com.autoconsultancy.dto.request.CreateWorkerRequest;
import com.autoconsultancy.dto.response.ApiResponse;
import com.autoconsultancy.dto.response.WorkerDashboardStats;
import com.autoconsultancy.entity.Worker;
import com.autoconsultancy.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/worker")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORKER')")
public class WorkerController {

    private final WorkerService workerService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Worker>> getProfile(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(workerService.getProfile(authentication.getName()), "Profile fetched"));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Worker>> updateProfile(@RequestBody CreateWorkerRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(workerService.updateProfile(authentication.getName(), request), "Profile updated"));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<WorkerDashboardStats>> getDashboardStats(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(workerService.getDashboardStats(authentication.getName()), "Dashboard fetched"));
    }
}
