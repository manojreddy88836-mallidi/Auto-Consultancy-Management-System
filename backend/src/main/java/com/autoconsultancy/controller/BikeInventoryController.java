package com.autoconsultancy.controller;

import com.autoconsultancy.dto.request.BikeInventoryRequest;
import com.autoconsultancy.dto.response.*;
import com.autoconsultancy.service.BikeInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/bikes")
@RequiredArgsConstructor
public class BikeInventoryController {

    private final BikeInventoryService service;

    // ─── PUBLIC endpoints ──────────────────────────────────────────────────

    /** Customer: browse available bikes (with images) */
    @GetMapping("/public/available")
    public ResponseEntity<ApiResponse<List<BikeInventoryResponse>>> getAvailable(
            @RequestParam(required = false) Long manufacturerId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getAvailableForCustomers(manufacturerId, search), "Available bikes"));
    }

    /** Customer: view single bike detail */
    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<BikeInventoryResponse>> getPublicDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id), "Bike detail"));
    }

    /** Serve image file (no auth — for img src) */
    @GetMapping("/{inventoryId}/images/{imageId}/file")
    public ResponseEntity<Resource> serveImage(
            @PathVariable Long inventoryId, @PathVariable Long imageId) {
        Resource res = service.serveImage(inventoryId, imageId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                .contentType(MediaType.IMAGE_JPEG)
                .body(res);
    }

    // ─── ADMIN / WORKER endpoints ──────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<PageResponse<BikeInventoryResponse>>> getAll(
            @RequestParam(required = false) Long modelId,
            @RequestParam(required = false) Long manufacturerId,
            @RequestParam(required = false) String saleStatus,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BikeInventoryResponse> result = service.getAll(
                modelId, manufacturerId, saleStatus, search,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result), "Bike inventory list"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<BikeInventoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id), "Bike detail"));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<BikeInventoryResponse>> create(
            @RequestBody BikeInventoryRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.create(req), "Bike created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<BikeInventoryResponse>> update(
            @PathVariable Long id, @RequestBody BikeInventoryRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req), "Bike updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Bike deleted"));
    }

    // ─── Sale status transitions ────────────────────────────────────────────

    @PutMapping("/{id}/enable-sale")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<BikeInventoryResponse>> enableSale(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.enableSale(id), "Sale enabled"));
    }

    @PutMapping("/{id}/disable-sale")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<BikeInventoryResponse>> disableSale(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.disableSale(id), "Sale disabled"));
    }

    @PutMapping("/{id}/reserve")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<BikeInventoryResponse>> reserve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.reserve(id), "Bike reserved"));
    }

    @PutMapping("/{id}/sold")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<BikeInventoryResponse>> markSold(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.markSold(id), "Bike marked as sold"));
    }

    // ─── Image management ──────────────────────────────────────────────────

    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<BikeInventoryImageResponse>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "setPrimary", defaultValue = "false") boolean setPrimary) {
        return ResponseEntity.ok(ApiResponse.success(
                service.uploadImage(id, file, setPrimary), "Image uploaded"));
    }

    @GetMapping("/{id}/images")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<List<BikeInventoryImageResponse>>> getImages(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getImages(id), "Images fetched"));
    }

    @PutMapping("/{id}/images/{imageId}/primary")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<BikeInventoryImageResponse>> setPrimary(
            @PathVariable Long id, @PathVariable Long imageId) {
        return ResponseEntity.ok(ApiResponse.success(service.setPrimary(id, imageId), "Primary set"));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long id, @PathVariable Long imageId) {
        service.deleteImage(id, imageId);
        return ResponseEntity.ok(ApiResponse.success(null, "Image deleted"));
    }
}
