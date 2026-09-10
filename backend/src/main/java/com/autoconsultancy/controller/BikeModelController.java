package com.autoconsultancy.controller;

import com.autoconsultancy.dto.request.BikeModelRequest;
import com.autoconsultancy.dto.request.BikeVariantRequest;
import com.autoconsultancy.dto.request.ManufacturingYearRequest;
import com.autoconsultancy.dto.response.ApiResponse;
import com.autoconsultancy.dto.response.BikeImageResponse;
import com.autoconsultancy.dto.response.BikeModelResponse;
import com.autoconsultancy.dto.response.BikeVariantResponse;
import com.autoconsultancy.dto.response.PageResponse;
import com.autoconsultancy.service.BikeModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bike-models")
@RequiredArgsConstructor
public class BikeModelController {

    private final BikeModelService bikeModelService;

    // ── Public endpoints ─────────────────────────────────────────────────

    /** Customer: bikes by manufacturer (for application form cascades) */
    @GetMapping("/public/by-manufacturer/{manufacturerId}")
    public ResponseEntity<ApiResponse<List<BikeModelResponse>>> getByManufacturerId(@PathVariable Long manufacturerId) {
        return ResponseEntity.ok(ApiResponse.success(bikeModelService.getByManufacturerId(manufacturerId), "Fetched bike models"));
    }

    @GetMapping("/public/{modelId}/variants")
    public ResponseEntity<ApiResponse<List<BikeVariantResponse>>> getVariants(@PathVariable Long modelId) {
        return ResponseEntity.ok(ApiResponse.success(bikeModelService.getVariants(modelId), "Fetched variants"));
    }

    @GetMapping("/public/{modelId}/years")
    public ResponseEntity<ApiResponse<List<Integer>>> getYears(@PathVariable Long modelId) {
        return ResponseEntity.ok(ApiResponse.success(bikeModelService.getYears(modelId), "Fetched years"));
    }

    /** Customer: bikes available for sale (AVAILABLE status only) */
    @GetMapping("/public/for-sale")
    public ResponseEntity<ApiResponse<List<BikeModelResponse>>> getForSale(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long manufacturerId) {
        return ResponseEntity.ok(ApiResponse.success(
                bikeModelService.getAvailableForSale(search, manufacturerId), "Fetched available bikes"));
    }

    /** Customer: single bike public detail */
    @GetMapping("/public/{id}/detail")
    public ResponseEntity<ApiResponse<BikeModelResponse>> getPublicDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bikeModelService.getPublicDetail(id), "Fetched bike detail"));
    }

    /** Public: serve image file (no auth required so images display in customer UI) */
    @GetMapping("/images/{imageId}/file")
    public ResponseEntity<Resource> serveImage(@PathVariable Long imageId) {
        Resource resource = bikeModelService.getImageFile(imageId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    // ── Admin endpoints ──────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<PageResponse<BikeModelResponse>>> getAll(
            @RequestParam(required = false) Long manufacturerId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<BikeModelResponse> response = PageResponse.of(
                bikeModelService.getAll(manufacturerId, search, PageRequest.of(page, size)));
        return ResponseEntity.ok(ApiResponse.success(response, "Fetched all bike models"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<BikeModelResponse>> create(@Valid @RequestBody BikeModelRequest request) {
        return ResponseEntity.ok(ApiResponse.success(bikeModelService.create(request), "Created bike model"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<BikeModelResponse>> update(@PathVariable Long id, @Valid @RequestBody BikeModelRequest request) {
        return ResponseEntity.ok(ApiResponse.success(bikeModelService.update(id, request), "Updated bike model"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        bikeModelService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deactivated bike model"));
    }

    /** Admin/Worker: update sale status */
    @PutMapping("/{id}/sale-status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<BikeModelResponse>> updateSaleStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("saleStatus");
        return ResponseEntity.ok(ApiResponse.success(bikeModelService.updateSaleStatus(id, status), "Sale status updated"));
    }

    /** Admin/Worker: upload bike image */
    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<BikeImageResponse>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "setPrimary", defaultValue = "false") boolean setPrimary) {
        return ResponseEntity.ok(ApiResponse.success(
                bikeModelService.uploadImage(id, file, setPrimary), "Image uploaded"));
    }

    /** Admin/Worker: get images for a model */
    @GetMapping("/{id}/images")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<List<BikeImageResponse>>> getImages(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bikeModelService.getImages(id), "Fetched images"));
    }

    /** Admin/Worker: set primary image */
    @PutMapping("/{id}/images/{imageId}/primary")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<BikeImageResponse>> setPrimary(
            @PathVariable Long id, @PathVariable Long imageId) {
        return ResponseEntity.ok(ApiResponse.success(
                bikeModelService.setPrimaryImage(id, imageId), "Primary image updated"));
    }

    /** Admin/Worker: delete image */
    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long id, @PathVariable Long imageId) {
        bikeModelService.deleteImage(id, imageId);
        return ResponseEntity.ok(ApiResponse.success(null, "Image deleted"));
    }

    // ── Variants / Years ─────────────────────────────────────────────────

    @PostMapping("/{modelId}/variants")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<BikeVariantResponse>> addVariant(@PathVariable Long modelId, @Valid @RequestBody BikeVariantRequest request) {
        return ResponseEntity.ok(ApiResponse.success(bikeModelService.addVariant(modelId, request), "Added variant"));
    }

    @PutMapping("/variants/{variantId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<BikeVariantResponse>> updateVariant(@PathVariable Long variantId, @Valid @RequestBody BikeVariantRequest request) {
        return ResponseEntity.ok(ApiResponse.success(bikeModelService.updateVariant(variantId, request), "Updated variant"));
    }

    @DeleteMapping("/variants/{variantId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable Long variantId) {
        bikeModelService.deleteVariant(variantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted variant"));
    }

    @PostMapping("/{modelId}/years")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addYear(@PathVariable Long modelId, @Valid @RequestBody ManufacturingYearRequest request) {
        bikeModelService.addYear(modelId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Added year"));
    }

    @DeleteMapping("/years/{yearId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteYear(@PathVariable Long yearId) {
        bikeModelService.deleteYear(yearId);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted year"));
    }
}
