package com.autoconsultancy.controller;

import com.autoconsultancy.dto.request.ManufacturerRequest;
import com.autoconsultancy.dto.response.ApiResponse;
import com.autoconsultancy.dto.response.PageResponse;
import com.autoconsultancy.entity.Manufacturer;
import com.autoconsultancy.service.ManufacturerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BrandController – primary /api/brands endpoint (user-facing: "Brands").
 * The underlying entity is still named Manufacturer internally, but the API
 * surface and all user-visible messages now consistently use "Brand".
 */
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final ManufacturerService manufacturerService;

    @GetMapping("/public/all")
    public ResponseEntity<ApiResponse<List<Manufacturer>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.success(manufacturerService.getAllActive(), "Fetched active brands"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<Manufacturer>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<Manufacturer> pageResponse = PageResponse.of(manufacturerService.getAll(PageRequest.of(page, size)));
        return ResponseEntity.ok(ApiResponse.success(pageResponse, "Fetched all brands"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Manufacturer>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(manufacturerService.getById(id), "Brand fetched"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Manufacturer>> create(@Valid @RequestBody ManufacturerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(manufacturerService.create(request), "Brand created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Manufacturer>> update(
            @PathVariable Long id,
            @Valid @RequestBody ManufacturerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(manufacturerService.update(id, request), "Brand updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        manufacturerService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Brand deactivated successfully"));
    }
}
