package com.autoconsultancy.controller;

import com.autoconsultancy.dto.request.*;
import com.autoconsultancy.dto.response.*;
import com.autoconsultancy.service.WorkerTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/worker/tasks")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORKER')")
public class WorkerTaskController {

    private final WorkerTaskService taskService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<WorkerTaskResponse>>> getMyTasks(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.getMyTasks(auth.getName(), type, status, page, size), "Tasks fetched"));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodaySummary(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.getTodaySummary(auth.getName()), "Today summary fetched"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkerTaskDetailResponse>> getTaskDetail(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.getMyTaskDetail(id, auth.getName()), "Task detail fetched"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<WorkerTaskResponse>> updateStatus(
            @PathVariable Long id, @RequestBody TaskStatusUpdateRequest req, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.updateStatus(id, req, auth.getName()), "Status updated"));
    }

    @PutMapping("/{id}/service")
    public ResponseEntity<ApiResponse<WorkerTaskDetailResponse>> updateServiceJob(
            @PathVariable Long id, @RequestBody ServiceJobUpdateRequest req, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.updateServiceJob(id, req, auth.getName()), "Service job updated"));
    }

    @PostMapping("/{id}/collect")
    public ResponseEntity<ApiResponse<WorkerTaskDetailResponse>> recordPayment(
            @PathVariable Long id, @RequestBody PaymentCollectionRequest req, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.recordPayment(id, req, auth.getName()), "Payment recorded"));
    }

    @PutMapping("/{id}/visit")
    public ResponseEntity<ApiResponse<WorkerTaskDetailResponse>> updateVisit(
            @PathVariable Long id, @RequestBody FieldVisitUpdateRequest req, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.updateFieldVisit(id, req, auth.getName()), "Visit updated"));
    }

    @PutMapping("/{id}/recovery")
    public ResponseEntity<ApiResponse<WorkerTaskDetailResponse>> updateRecovery(
            @PathVariable Long id, @RequestBody BikeRecoveryUpdateRequest req, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.updateRecovery(id, req, auth.getName()), "Recovery updated"));
    }
}
