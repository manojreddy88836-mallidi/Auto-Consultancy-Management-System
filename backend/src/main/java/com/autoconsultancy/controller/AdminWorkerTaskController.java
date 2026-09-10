package com.autoconsultancy.controller;

import com.autoconsultancy.dto.request.WorkerTaskRequest;
import com.autoconsultancy.dto.response.*;
import com.autoconsultancy.service.WorkerTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/worker-tasks")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminWorkerTaskController {

    private final WorkerTaskService taskService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<WorkerTaskResponse>>> getAllTasks(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long workerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.adminGetAllTasks(type, status, workerId, page, size), "Tasks fetched"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkerTaskResponse>> createTask(
            @RequestBody WorkerTaskRequest req, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.adminCreateTask(req, auth.getName()), "Task created"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkerTaskDetailResponse>> getTaskDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.adminGetTaskDetail(id), "Task detail fetched"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkerTaskResponse>> updateTask(
            @PathVariable Long id, @RequestBody WorkerTaskRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.adminUpdateTask(id, req), "Task updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelTask(@PathVariable Long id) {
        taskService.adminCancelTask(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Task cancelled"));
    }
}
