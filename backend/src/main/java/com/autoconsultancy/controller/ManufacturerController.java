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
 * ManufacturerController — DEPRECATED backward-compatibility shim.
 *
 * <p>The user-facing terminology has been globally updated from "Manufacturer"
 * to "Brand". All active frontend and API consumers should use the canonical
 * {@link BrandController} at {@code /api/brands} instead.</p>
 *
 * <p>This controller is kept to avoid breaking any legacy integrations or
 * external consumers that may still call {@code /api/manufacturers/*}.
 * It delegates every request to the shared {@link ManufacturerService}
 * (the underlying service/entity name has not changed).
 * It may be removed in a future major release.</p>
 *
 * @deprecated Use {@link BrandController} at {@code /api/brands}
 */
@Deprecated
@RestController
@RequestMapping("/api/manufacturers")
@RequiredArgsConstructor
public class ManufacturerController {

    private final ManufacturerService manufacturerService;

    /** @deprecated Use {@code GET /api/brands/public/all} */
    @Deprecated
    @GetMapping("/public/all")
    public ResponseEntity<ApiResponse<List<Manufacturer>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.success(
                manufacturerService.getAllActive(),
                "Fetched active brands (legacy endpoint – use /api/brands/public/all)"));
    }

    /** @deprecated Use {@code GET /api/brands} */
    @Deprecated
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<Manufacturer>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<Manufacturer> pageResponse =
                PageResponse.of(manufacturerService.getAll(PageRequest.of(page, size)));
        return ResponseEntity.ok(ApiResponse.success(
                pageResponse,
                "Fetched all brands (legacy endpoint – use /api/brands)"));
    }

    /** @deprecated Use {@code POST /api/brands} */
    @Deprecated
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Manufacturer>> create(
            @Valid @RequestBody ManufacturerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                manufacturerService.create(request),
                "Brand created (legacy endpoint – use /api/brands)"));
    }

    /** @deprecated Use {@code PUT /api/brands/{id}} */
    @Deprecated
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Manufacturer>> update(
            @PathVariable Long id,
            @Valid @RequestBody ManufacturerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                manufacturerService.update(id, request),
                "Brand updated (legacy endpoint – use /api/brands/{id})"));
    }

    /** @deprecated Use {@code DELETE /api/brands/{id}} */
    @Deprecated
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        manufacturerService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(
                null,
                "Brand deactivated (legacy endpoint – use /api/brands/{id})"));
    }
}
