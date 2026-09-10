package com.autoconsultancy.controller;

import com.autoconsultancy.dto.request.BikeOfferRequest;
import com.autoconsultancy.dto.request.OfferRespondRequest;
import com.autoconsultancy.dto.response.ApiResponse;
import com.autoconsultancy.dto.response.BikeOfferResponse;
import com.autoconsultancy.service.BikeOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class BikeOfferController {

    private final BikeOfferService offerService;

    // ── Customer ──────────────────────────────────────────────────────────

    /** Submit a new price offer for a specific bike */
    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<BikeOfferResponse>> submitOffer(
            @RequestBody BikeOfferRequest request,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                offerService.submitOffer(auth.getName(), request), "Offer submitted successfully"));
    }

    /** Get all offers made by the authenticated customer */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<BikeOfferResponse>>> getMyOffers(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                offerService.getMyOffers(auth.getName()), "My offers"));
    }

    /** Get my latest offer on a specific bike (for BikeDetailPage display) */
    @GetMapping("/my/bike/{bikeInventoryId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<BikeOfferResponse>> getMyOfferForBike(
            @PathVariable Long bikeInventoryId,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                offerService.getMyOfferForBike(auth.getName(), bikeInventoryId),
                "Offer for bike"));
    }

    /** Customer withdraws a PENDING or COUNTER_OFFER offer */
    @PutMapping("/{id}/withdraw")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<BikeOfferResponse>> withdrawOffer(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                offerService.withdrawOffer(id, auth.getName()), "Offer withdrawn"));
    }

    /** Customer accepts or rejects a counter offer */
    @PutMapping("/{id}/counter-response")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<BikeOfferResponse>> respondToCounter(
            @PathVariable Long id,
            @RequestParam boolean accept,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                offerService.respondToCounter(id, accept, auth.getName()),
                accept ? "Counter offer accepted" : "Counter offer rejected"));
    }

    // ── Admin / Worker ─────────────────────────────────────────────────────

    /** Paginated list of all offers — admin and worker can see all */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<Page<BikeOfferResponse>>> getAllOffers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            Authentication auth) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                offerService.getAllOffers(status, pr), "All offers"));
    }

    /** Admin/worker responds: ACCEPTED, REJECTED, or COUNTER_OFFER */
    @PutMapping("/{id}/respond")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<BikeOfferResponse>> respondToOffer(
            @PathVariable Long id,
            @RequestBody OfferRespondRequest request,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                offerService.respondToOffer(id, request, auth.getName()),
                "Response recorded"));
    }

    /** Get a single offer by ID — customer sees only own, admin/worker sees all */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<BikeOfferResponse>> getOfferById(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                offerService.getOfferById(id, auth.getName()), "Offer detail"));
    }
}
